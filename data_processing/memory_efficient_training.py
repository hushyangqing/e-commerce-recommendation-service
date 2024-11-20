import pandas as pd
import numpy as np
from surprise import Dataset, Reader, SVD, accuracy
import mysql.connector
import joblib
from tqdm import tqdm
import gc
import os
from pathlib import Path
from dotenv import load_dotenv
import json
from concurrent.futures import ProcessPoolExecutor, ThreadPoolExecutor, as_completed

load_dotenv()

def sanitize_string(value):
        if value is None:
            return ''
        try:
            # Ensure the value is a string
            value = str(value)
            # Encode as UTF-8, ignoring invalid characters
            value = value.encode('utf-8', errors='ignore').decode('utf-8')
            return value
        except Exception as e:
            print(f"Error sanitizing value {value}: {e}")
            return ''
        
class MemoryEfficientRecommender:
    def __init__(self, sample_size=10000):
        self.sample_size = sample_size
        self.combined_model = None
        self.category_models = {}
        self.model_dir = Path('models')
        self.temp_dir = Path('temp_data')
        self.test_user_id = None  # Will store a valid user_id from training data
        
        self.model_dir.mkdir(exist_ok=True)
        self.temp_dir.mkdir(exist_ok=True)
        
        self.connection = mysql.connector.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            database=os.getenv('DB_NAME'),
            port=os.getenv('DB_PORT'),
            connection_timeout=120
        )
        
        self.categories = {
            'beauty': ('All_Beauty', 'meta_All_Beauty'),
            'fashion': ('Amazon_Fashion', 'meta_Amazon_Fashion'),
            'appliances': ('Appliances', 'meta_Appliances'),
            'arts_crafts': ('Arts_Crafts_and_Sewing', 'meta_Arts_Crafts_and_Sewing'),
            'automotive': ('Automotive', 'meta_Automotive')
        }
        
    def process_data_in_chunks(self, category=None):

        users_json_path = self.temp_dir / "users.json"
        products_json_path = self.temp_dir / "products.json"
        self.temp_dir.mkdir(exist_ok=True)

        if users_json_path.exists():
            with open(users_json_path, "r", encoding="utf-8") as users_file:
                all_users = json.load(users_file)
        else:
            all_users = []

        if products_json_path.exists():
            with open(products_json_path, "r", encoding="utf-8") as products_file:
                all_products = json.load(products_file)
        else:
            all_products = []

        user_enum_start = len(all_users)
        chunk_files = []

        if category:
            review_table, meta_table = self.categories[category]
            tables_to_process = [(review_table, meta_table)]
        else:
            tables_to_process = [tables for tables in self.categories.values()]
        
        for review_table, meta_table in tables_to_process:
            query = f"""
                SELECT 
                    r.rating, 
                    r.user_id, 
                    r.parent_asin,
                    m.title,
                    m.average_rating,
                    m.rating_number,
                    m.price
                FROM {review_table} r
                JOIN {meta_table} m ON r.parent_asin = m.parent_asin
                WHERE r.rating IS NOT NULL
                AND r.user_id IS NOT NULL
                AND r.parent_asin IS NOT NULL
                AND m.title IS NOT NULL
                AND m.price IS NOT NULL
                LIMIT {self.sample_size}
            """
            
            chunk_df = pd.read_sql(query, self.connection)
            
            if not chunk_df.empty:
                #create sample user table and product json
                users = chunk_df[['user_id']].drop_duplicates()
                products = chunk_df[['parent_asin', 'title', 'price', 'average_rating', 'rating_number']].drop_duplicates()

                new_users = [{"user_id": row.user_id, "username": f"user_{i}"}
                            for i, row in enumerate(users.itertuples(index=False), start=user_enum_start + 1)]
                user_enum_start += len(new_users) 
                all_users.extend(new_users)

                all_products.extend([{
                    "parent_asin": row.parent_asin,
                    "title": row.title,
                    "price": row.price,
                    "average_rating": row.average_rating,
                    "rating_number": row.rating_number,
                    "category": review_table
                } for row in products.itertuples(index=False)])

                chunk_df = chunk_df[(chunk_df['rating'] >= 1) & (chunk_df['rating'] <= 5)].copy()
                
                # Store a valid user_id for testing
                if self.test_user_id is None and not chunk_df.empty:
                    self.test_user_id = chunk_df['user_id'].iloc[0]
                
                prefix = category if category else 'combined'
                chunk_file = self.temp_dir / f"{prefix}_chunk.parquet"
                chunk_df.to_parquet(chunk_file)
                chunk_files.append(chunk_file)
            
            del chunk_df
            gc.collect()

        with open(users_json_path, "w", encoding="utf-8") as users_file:
            json.dump(all_users, users_file, ensure_ascii=False, indent=4)

        with open(products_json_path, "w", encoding="utf-8") as products_file:
            json.dump(all_products, products_file, ensure_ascii=False, indent=4)

        print(f"Users and products dumped to {users_json_path} and {products_json_path}")

        return chunk_files
    
    def save_users_products(self):
        users_json_path = self.temp_dir / "users.json"
        products_json_path = self.temp_dir / "products.json"

        service_conn = mysql.connector.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            port=os.getenv('DB_PORT'),
            database="service_data"
        )
        cursor = service_conn.cursor()

        # Create tables if not exist
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS users (
                user_id VARCHAR(100) PRIMARY KEY,
                username VARCHAR(50),
                password VARCHAR(100) DEFAULT 'password'
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS products (
                parent_asin VARCHAR(50) PRIMARY KEY,
                title TEXT,
                price FLOAT,
                average_rating FLOAT,
                rating_number INT,
                category VARCHAR(50)
            )
        """)

        # Load users from JSON and insert into MySQL
        with open(users_json_path, "r", encoding="utf-8") as users_file:
            all_users = json.load(users_file)

        user_data = [(user['user_id'], user['username']) for user in all_users]
        cursor.executemany(
            "INSERT IGNORE INTO users (user_id, username) VALUES (%s, %s)",
            user_data
        )

        # Load products from JSON and insert into MySQL
        with open(products_json_path, "r", encoding="utf-8") as products_file:
            all_products = json.load(products_file)

        product_data = [(product['parent_asin'], product['title'], product['price'],
                        product['average_rating'], product['rating_number'], product['category']) 
                        for product in all_products]
        cursor.executemany(
            "INSERT IGNORE INTO products (parent_asin, title, price, average_rating, rating_number, category) VALUES (%s, %s, %s, %s, %s, %s)",
            product_data
        )

        service_conn.commit()
        service_conn.close()
        print("Users and products saved to MySQL")

    def train_model(self, chunk_files, category=None):
        model = SVD(n_factors=100, n_epochs=20, lr_all=0.005, reg_all=0.02, random_state=42)
        reader = Reader(rating_scale=(1, 5))
        
        for chunk_file in chunk_files:
            chunk_df = pd.read_parquet(chunk_file)
            data = Dataset.load_from_df(chunk_df[['user_id', 'parent_asin', 'rating']], reader)
            trainset = data.build_full_trainset()
            model.fit(trainset)
            
            del chunk_df, data, trainset
            gc.collect()
        
        return model

    def save_models(self):
        if self.combined_model:
            combined_path = self.model_dir / 'combined_model.joblib'
            joblib.dump(self.combined_model, combined_path)
        
        for category, model in self.category_models.items():
            model_path = self.model_dir / f"{category}_model.joblib"
            joblib.dump(model, model_path)
            
        # Save test_user_id
        with open(self.model_dir / 'test_user_id.txt', 'w') as f:
            f.write(self.test_user_id)

    def load_models(self):
        combined_path = self.model_dir / 'combined_model.joblib'
        if combined_path.exists():
            self.combined_model = joblib.load(combined_path)
        
        for category in self.categories:
            model_path = self.model_dir / f"{category}_model.joblib"
            if model_path.exists():
                self.category_models[category] = joblib.load(model_path)
                
        # Load test_user_id
        user_id_path = self.model_dir / 'test_user_id.txt'
        if user_id_path.exists():
            with open(user_id_path, 'r') as f:
                self.test_user_id = f.read().strip()

    def train_all_models(self):
        try:
            chunk_files = self.process_data_in_chunks()
            self.combined_model = self.train_model(chunk_files)
            
            for category in self.categories:
                chunk_files = self.process_data_in_chunks(category)
                self.category_models[category] = self.train_model(chunk_files, category)
            
            self.save_models()
            print("Models saved successfully")
            
        except Exception as e:
            raise e

    def get_recommendations(self, user_id, category=None, n=10):
        if category and category not in self.categories:
            raise ValueError(f"Invalid category: {category}")
        
        if category:
            if category not in self.category_models:
                raise ValueError(f"No model found for category: {category}")
            model = self.category_models[category]
            review_table, meta_table = self.categories[category]
            query = f"SELECT DISTINCT parent_asin FROM {meta_table} LIMIT {self.sample_size}"
        else:
            if self.combined_model is None:
                raise ValueError("Combined model not found")
            model = self.combined_model
            query = " UNION ".join(
                f"(SELECT DISTINCT parent_asin FROM {meta_table} LIMIT {self.sample_size})"
                for _, meta_table in self.categories.values()
            )
        
        cursor = self.connection.cursor()
        cursor.execute(query)
        products = [row[0] for row in cursor.fetchall()]
        predictions = []
        
        for product_id in products:
            try:
                pred = model.predict(user_id, product_id)
                predictions.append((product_id, pred.est))
            except:
                continue
        
        predictions.sort(key=lambda x: x[1], reverse=True)
        return [p[0] for p in predictions[:n]]
    
    def process_recommendations(self, user_id):
        try:
            product_ids = self.get_recommendations(user_id, n=10)
            general_recs = (user_id, json.dumps(product_ids))

            category_recs = []
            for category in self.categories:
                product_ids = self.get_recommendations(user_id, category=category, n=10)
                category_recs.append((user_id, category, json.dumps(product_ids)))

            return general_recs, category_recs
        except Exception as e:
            print(f"Error generating recommendations for user {user_id}: {e}")
            return None, []
        
    def save_recommendations(self):
        service_conn = mysql.connector.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            port=os.getenv('DB_PORT'),
            database="service_data"
        )
        cursor = service_conn.cursor()

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS recommendations (
                user_id VARCHAR(100) PRIMARY KEY,
                product_list JSON
            )
        """)

        cursor.execute("""
            CREATE TABLE IF NOT EXISTS category_recommendations (
                user_id VARCHAR(100),
                category VARCHAR(50),
                product_list JSON,
                PRIMARY KEY (user_id, category)
            )
        """)

        cursor.execute("SELECT user_id FROM users LIMIT 1000")
        users = [row[0] for row in cursor.fetchall()]

        general_recs_batch = []
        category_recs_batch = []

        # with ThreadPoolExecutor(max_workers=2) as executor: 
        #     futures = {
        #         executor.submit(self.process_recommendations, user_id): user_id
        #         for user_id in users
        #     }

        #     for future in tqdm(as_completed(futures), total=len(futures), desc="Processing user recommendations"):
        #         general_rec, category_recs = future.result()
        #         if general_rec:
        #             general_recs_batch.append(general_rec)
        #         category_recs_batch.extend(category_recs)

        for user_id in tqdm(users, desc="Processing user recommendations"):
            try:
                product_ids = self.get_recommendations(user_id, n=10)
                general_recs_batch.append((user_id, json.dumps(product_ids)))

                for category in self.categories:
                    product_ids = self.get_recommendations(user_id, category=category, n=10)
                    category_recs_batch.append((user_id, category, json.dumps(product_ids)))

            except Exception as e:
                print(f"Error generating recommendations for user {user_id}: {e}")

        try:
            cursor.executemany(
                "INSERT IGNORE INTO recommendations (user_id, product_list) VALUES (%s, %s)",
                general_recs_batch
            )
        except mysql.connector.Error as e:
            print(f"Error inserting general recommendations: {e}")

        try:
            cursor.executemany(
                "INSERT IGNORE INTO category_recommendations (user_id, category, product_list) VALUES (%s, %s, %s)",
                category_recs_batch
            )
        except mysql.connector.Error as e:
            print(f"Error inserting category recommendations: {e}")

        service_conn.commit()
        service_conn.close()

if __name__ == "__main__":
    recommender = MemoryEfficientRecommender(sample_size=50000)
    
    try:
        print("Attempting to load existing models...")
        try:
            combined_path = Path('models/combined_model.joblib')
            if not combined_path.exists():
                raise FileNotFoundError("Combined model not found")
            recommender.load_models()
            print("Existing models loaded successfully")
            print(f"Using test user_id: {recommender.test_user_id}")
        except FileNotFoundError as e:
            print(f"No existing models found: {e}")
            print("Training new models...")
            recommender.train_all_models()
            print("Models trained successfully")
            recommender.save_users_products()
            print("Users and Products saved successfully")
            print(f"Saved test user_id: {recommender.test_user_id}")
        
        general_recs = recommender.get_recommendations(recommender.test_user_id, n=10)
        print("General recommendations:", general_recs)
        
        for category in recommender.categories:
            category_recs = recommender.get_recommendations(recommender.test_user_id, category=category, n=10)
            print(f"{category} recommendations:", category_recs)
        
        print("Saving recommendations:")
        recommender.save_recommendations()

    except Exception as e:
        print(f"Error: {str(e)}")
    finally:
        if recommender.connection.is_connected():
            recommender.connection.close()