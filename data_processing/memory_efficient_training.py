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

load_dotenv()

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
                    m.average_rating,
                    m.rating_number,
                    m.price
                FROM {review_table} r
                JOIN {meta_table} m ON r.parent_asin = m.parent_asin
                WHERE r.rating IS NOT NULL
                AND r.user_id IS NOT NULL
                AND r.parent_asin IS NOT NULL
                LIMIT {self.sample_size}
            """
            
            chunk_df = pd.read_sql(query, self.connection)
            
            if not chunk_df.empty:
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
        
        return chunk_files

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
        finally:
            if self.temp_dir.exists():
                for file in self.temp_dir.glob('*'):
                    file.unlink()

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
            print(f"Saved test user_id: {recommender.test_user_id}")
        
        general_recs = recommender.get_recommendations(recommender.test_user_id, n=10)
        print("General recommendations:", general_recs)
        
        for category in recommender.categories:
            category_recs = recommender.get_recommendations(recommender.test_user_id, category=category, n=5)
            print(f"{category} recommendations:", category_recs)
        
    except Exception as e:
        print(f"Error: {str(e)}")
    finally:
        if recommender.connection.is_connected():
            recommender.connection.close()