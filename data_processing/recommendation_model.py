import mysql.connector
import pandas as pd
import numpy as np
from surprise import Dataset, Reader, SVD, accuracy
from surprise.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics.pairwise import cosine_similarity
import json
import os
from dotenv import load_dotenv
from typing import Optional, List, Dict, Any
from pathlib import Path
import joblib

load_dotenv()

class HybridRecommendationSystem:
    def __init__(self, use_combined_model=True, model_dir='models'):
        self.use_combined_model = use_combined_model
        self.models = {}
        self.combined_model = None
        self.feature_matrices = {}
        self.scalers = {}
        self.category_tables = {
            'beauty': ('All_Beauty', 'meta_All_Beauty'),
            'fashion': ('Amazon_Fashion', 'meta_Amazon_Fashion'),
            'appliances': ('Appliances', 'meta_Appliances'),
            'arts_crafts': ('Arts_Crafts_and_Sewing', 'meta_Arts_Crafts_and_Sewing'),
            'automotive': ('Automotive', 'meta_Automotive')
        }
        self.model_dir = Path(model_dir)
        self.model_dir.mkdir(parents=True, exist_ok=True)

    def get_connection(self):
        connection = mysql.connector.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            database=os.getenv('DB_NAME'),
            port=os.getenv('DB_PORT'),
            connection_timeout=120
        )
        return connection
    
    def safe_json_loads(self, json_str: Optional[str], default_value: Any = None) -> Any:
        if not json_str or json_str == 'null':
            return default_value
        try:
            return json.loads(json_str)
        except:
            return default_value

    def safe_str(self, value: Any) -> str:
        if pd.isna(value) or value is None:
            return ''
        return str(value).strip()

    def safe_float(self, value: Any) -> float:
        try:
            if pd.isna(value) or value is None:
                return 0.0
            return float(value)
        except:
            return 0.0

    def extract_features_from_json(self, json_data: Any, field_name: str) -> List[str]:
        if pd.isna(json_data) or not json_data:
            return []

        try:
            data = self.safe_json_loads(json_data) if isinstance(json_data, str) else json_data
            if not data:
                return []

            if field_name == 'features':
                return [self.safe_str(feature).lower() for feature in data if feature]
            elif field_name == 'description':
                if isinstance(data, list):
                    return [self.safe_str(d).lower() for d in data if d]
                return [self.safe_str(data).lower()]
            elif field_name == 'categories':
                categories = []
                if isinstance(data, list):
                    for cat in data:
                        if isinstance(cat, list):
                            categories.extend([self.safe_str(c).lower() for c in cat if c])
                        else:
                            if cat:
                                categories.append(self.safe_str(cat).lower())
                else:
                    if data:
                        categories.append(self.safe_str(data).lower())
                return categories
        except Exception as e:
            print(f"Error processing {field_name}: {str(e)}")
            return []
        return []

    def get_data_from_mysql(self, category: Optional[str] = None) -> pd.DataFrame:
        try:
            connection = self.get_connection()
            all_data = []
            
            categories = [category] if category else self.category_tables.keys()
            
            for cat in categories:
                review_table, meta_table = self.category_tables[cat]
                
                query = f"""
                    SELECT 
                        COALESCE(r.rating, 0) as rating,
                        r.user_id,
                        r.asin,
                        r.parent_asin,
                        COALESCE(r.title, '') as review_title,
                        COALESCE(r.text, '') as review_text,
                        '{cat}' as category,
                        COALESCE(m.main_category, '') as main_category,
                        COALESCE(m.title, '') as product_title,
                        COALESCE(m.average_rating, 0) as average_rating,
                        COALESCE(m.rating_number, 0) as rating_number,
                        m.features,
                        m.description,
                        COALESCE(m.price, 0) as price,
                        m.categories
                    FROM {review_table} r
                    JOIN {meta_table} m ON r.parent_asin = m.parent_asin
                    WHERE r.user_id IS NOT NULL
                    AND r.parent_asin IS NOT NULL
                """
                
                df = pd.read_sql(query, connection)
                df = df.fillna({
                    'rating': 0,
                    'review_title': '',
                    'review_text': '',
                    'main_category': '',
                    'product_title': '',
                    'average_rating': 0,
                    'rating_number': 0,
                    'price': 0,
                    'features': '[]',
                    'description': '[]',
                    'categories': '[]'
                })
                
                # Process text and JSON columns with null handling
                df['extracted_features'] = df['features'].apply(
                    lambda x: self.extract_features_from_json(x, 'features')
                )
                df['extracted_description'] = df['description'].apply(
                    lambda x: self.extract_features_from_json(x, 'description')
                )
                df['extracted_categories'] = df['categories'].apply(
                    lambda x: self.extract_features_from_json(x, 'categories')
                )
                
                # Clean numerical values
                df['rating'] = df['rating'].apply(self.safe_float)
                df['average_rating'] = df['average_rating'].apply(self.safe_float)
                df['rating_number'] = df['rating_number'].apply(lambda x: max(0, int(self.safe_float(x))))
                df['price'] = df['price'].apply(self.safe_float)
                
                all_data.append(df)
            
            combined_df = pd.concat(all_data, ignore_index=True)
            
            # Filter out rows with invalid ratings
            combined_df = combined_df[
                (combined_df['rating'] >= 1) & 
                (combined_df['rating'] <= 5)
            ]
            
            return combined_df
            
        finally:
            if 'connection' in locals() and connection.is_connected():
                connection.close()

    def create_feature_matrix(self, df: pd.DataFrame, category: str):
        try:
            numerical_features = pd.DataFrame({
                'average_rating': df['average_rating'].fillna(0),
                'rating_number': df['rating_number'].fillna(0),
                'price': df['price'].fillna(0)
            })
            
            scaler = StandardScaler()
            scaled_features = scaler.fit_transform(numerical_features)
            self.scalers[category] = scaler
            
            from sklearn.feature_extraction.text import TfidfVectorizer
            df['combined_text'] = df.apply(
                lambda row: ' '.join(filter(None, [
                    ' '.join(row['extracted_features']),
                    ' '.join(row['extracted_description']),
                    ' '.join(row['extracted_categories']),
                    self.safe_str(row['product_title'])
                ])),
                axis=1
            )
            
            tfidf = TfidfVectorizer(
                max_features=1000,
                stop_words='english',
                ngram_range=(1, 2),
                min_df=2 
            )
            
            if df['combined_text'].str.strip().str.len().sum() == 0:
                print(f"Warning: No text features found for {category}")
                return scaled_features
            
            text_features = tfidf.fit_transform(df['combined_text'])
            
            from scipy.sparse import hstack, csr_matrix
            scaled_features_sparse = csr_matrix(scaled_features)
            feature_matrix = hstack([text_features, scaled_features_sparse]).tocsr()
            
            return feature_matrix
            
        except Exception as e:
            print(f"Error creating feature matrix for {category}: {str(e)}")
            # Return only numerical features if text processing fails
            return csr_matrix(scaled_features)

    def train_models(self):
        try:
            if self.use_combined_model:
                df = self.get_data_from_mysql()
                
                if df.empty:
                    raise ValueError("No valid training data found")
                
                # Filter valid ratings
                valid_ratings = df[
                    (df['rating'] >= 1) & 
                    (df['rating'] <= 5) &
                    df['user_id'].notna() &
                    df['parent_asin'].notna()
                ]
                
                if valid_ratings.empty:
                    raise ValueError("No valid ratings found")
                
                # Train collaborative filtering model
                reader = Reader(rating_scale=(1, 5))
                data = Dataset.load_from_df(
                    valid_ratings[['user_id', 'parent_asin', 'rating']], 
                    reader
                )
                
                trainset, testset = train_test_split(data, test_size=0.25, random_state=42)
                
                self.combined_model = SVD(
                    n_factors=100,
                    n_epochs=20,
                    lr_all=0.005,
                    reg_all=0.02,
                    random_state=42
                )
                self.combined_model.fit(trainset)
                
                # Create content-based features
                self.feature_matrices['combined'] = self.create_feature_matrix(df, 'combined')
                
                # Evaluate model
                predictions = self.combined_model.test(testset)
                print(f"Combined model RMSE: {accuracy.rmse(predictions)}")
                print(f"Combined model MAE: {accuracy.mae(predictions)}")
                
            else:
                for category in self.category_tables.keys():
                    try:
                        df = self.get_data_from_mysql(category)
                        
                        if df.empty:
                            print(f"No data found for {category}, skipping...")
                            continue
                        
                        # Filter valid ratings
                        valid_ratings = df[
                            (df['rating'] >= 1) & 
                            (df['rating'] <= 5) &
                            df['user_id'].notna() &
                            df['parent_asin'].notna()
                        ]
                        
                        if valid_ratings.empty:
                            print(f"No valid ratings found for {category}, skipping...")
                            continue
                        
                        # Train models
                        reader = Reader(rating_scale=(1, 5))
                        data = Dataset.load_from_df(
                            valid_ratings[['user_id', 'parent_asin', 'rating']], 
                            reader
                        )
                        
                        trainset, testset = train_test_split(data, test_size=0.25, random_state=42)
                        
                        self.models[category] = SVD(
                            n_factors=100,
                            n_epochs=20,
                            lr_all=0.005,
                            reg_all=0.02,
                            random_state=42
                        )
                        self.models[category].fit(trainset)
                        
                        # Create content-based features
                        self.feature_matrices[category] = self.create_feature_matrix(df, category)
                        
                        # Evaluate model
                        predictions = self.models[category].test(testset)
                        print(f"{category} model RMSE: {accuracy.rmse(predictions)}")
                        print(f"{category} model MAE: {accuracy.mae(predictions)}")
                        
                    except Exception as e:
                        print(f"Error training model for {category}: {str(e)}")
                        continue
                        
        except Exception as e:
            print(f"Error training models: {str(e)}")
            raise

    def get_recommendations_for_user(self, user_id, category=None, n=10):
            try:
                connection = self.get_connection()
                
                if self.use_combined_model:
                    if self.combined_model is None:
                        raise ValueError("Model not trained")
                    model = self.combined_model
                    feature_matrix = self.feature_matrices['combined']
                    scaler = self.scalers['combined']
                else:
                    if category not in self.models:
                        raise ValueError(f"No model for category: {category}")
                    model = self.models[category]
                    feature_matrix = self.feature_matrices[category]
                    scaler = self.scalers[category]
                
                # Get candidate products
                if category:
                    _, meta_table = self.category_tables[category]
                    query = f"""
                        SELECT 
                            parent_asin,
                            average_rating,
                            rating_number,
                            price
                        FROM {meta_table}
                    """
                else:
                    query = " UNION ".join([
                        f"""
                        SELECT 
                            parent_asin,
                            average_rating,
                            rating_number,
                            price
                        FROM {meta_table}
                        """
                        for _, meta_table in self.category_tables.values()
                    ])
                
                products_df = pd.read_sql(query, connection)
                
                if products_df.empty:
                    return []
                
                # Get collaborative filtering predictions
                cf_predictions = []
                for product_id in products_df['parent_asin']:
                    try:
                        pred = model.predict(user_id, product_id)
                        cf_predictions.append((product_id, pred.est))
                    except:
                        continue
                
                # Get content-based scores
                cb_scores = []
                for idx, product in products_df.iterrows():
                    try:
                        # Scale numerical features
                        scaled_features = scaler.transform([[
                            product['average_rating'],
                            product['rating_number'],
                            product['price']
                        ]])
                        
                        # Calculate content-based similarity
                        similarity = cosine_similarity(
                            scaled_features,
                            feature_matrix[idx:idx+1]
                        )[0][0]
                        
                        cb_scores.append((product['parent_asin'], similarity))
                    except:
                        continue
                
                # Combine scores (50% CF, 50% content-based)
                final_scores = {}
                
                # Normalize CF scores
                if cf_predictions:
                    cf_min = min(score for _, score in cf_predictions)
                    cf_max = max(score for _, score in cf_predictions)
                    cf_range = cf_max - cf_min if cf_max > cf_min else 1
                    
                    for pid, score in cf_predictions:
                        normalized_score = (score - cf_min) / cf_range
                        final_scores[pid] = normalized_score * 0.5
                
                # Normalize and add content-based scores
                if cb_scores:
                    cb_min = min(score for _, score in cb_scores)
                    cb_max = max(score for _, score in cb_scores)
                    cb_range = cb_max - cb_min if cb_max > cb_min else 1
                    
                    for pid, score in cb_scores:
                        normalized_score = (score - cb_min) / cb_range
                        final_scores[pid] = final_scores.get(pid, 0) + (normalized_score * 0.5)
                
                # Sort and return top N recommendations
                recommendations = sorted(
                    final_scores.items(), 
                    key=lambda x: x[1], 
                    reverse=True
                )
                
                return [rec[0] for rec in recommendations[:n]]
                
            finally:
                if 'connection' in locals() and connection.is_connected():
                    connection.close()

    def save_models(self):
        try:
            if self.use_combined_model:
                if self.combined_model:
                    model_path = self.model_dir / 'combined_model.joblib'
                    scaler_path = self.model_dir / 'combined_scaler.joblib'
                    features_path = self.model_dir / 'combined_features.npz'
                    
                    joblib.dump(self.combined_model, model_path)
                    
                    if 'combined' in self.scalers:
                        joblib.dump(self.scalers['combined'], scaler_path)
                    
                    if 'combined' in self.feature_matrices:
                        from scipy.sparse import save_npz
                        save_npz(str(features_path), self.feature_matrices['combined'])
                    
                    print("Combined model saved successfully")
            else:
                for category in self.category_tables.keys():
                    if category in self.models:
                        category_dir = self.model_dir / category
                        category_dir.mkdir(exist_ok=True)
                        
                        model_path = category_dir / 'model.joblib'
                        scaler_path = category_dir / 'scaler.joblib'
                        features_path = category_dir / 'features.npz'
                        
                        joblib.dump(self.models[category], model_path)
                        
                        if category in self.scalers:
                            joblib.dump(self.scalers[category], scaler_path)
                        
                        if category in self.feature_matrices:
                            from scipy.sparse import save_npz
                            save_npz(str(features_path), self.feature_matrices[category])
                        
                        print(f"Model for category {category} saved successfully")
            
            config = {
                'use_combined_model': self.use_combined_model,
                'category_tables': self.category_tables
            }
            joblib.dump(config, self.model_dir / 'config.joblib')
            
        except Exception as e:
            print(f"Error saving models: {str(e)}")
            raise

    def load_models(self):
        try:
            config_path = self.model_dir / 'config.joblib'
            if not config_path.exists():
                raise FileNotFoundError("No saved models found")
            
            config = joblib.load(config_path)
            self.use_combined_model = config['use_combined_model']
            
            if self.use_combined_model:
                model_path = self.model_dir / 'combined_model.joblib'
                scaler_path = self.model_dir / 'combined_scaler.joblib'
                features_path = self.model_dir / 'combined_features.npz'
                
                if model_path.exists():
                    self.combined_model = joblib.load(model_path)
                if scaler_path.exists():
                    self.scalers['combined'] = joblib.load(scaler_path)
                if features_path.exists():
                    from scipy.sparse import load_npz
                    self.feature_matrices['combined'] = load_npz(str(features_path))
                
                print("Combined model loaded successfully")
            else:
                for category in self.category_tables.keys():
                    category_dir = self.model_dir / category
                    if not category_dir.exists():
                        continue
                    
                    model_path = category_dir / 'model.joblib'
                    scaler_path = category_dir / 'scaler.joblib'
                    features_path = category_dir / 'features.npz'
                    
                    if model_path.exists():
                        self.models[category] = joblib.load(model_path)
                    if scaler_path.exists():
                        self.scalers[category] = joblib.load(scaler_path)
                    if features_path.exists():
                        from scipy.sparse import load_npz
                        self.feature_matrices[category] = load_npz(str(features_path))
                    
                    print(f"Model for category {category} loaded successfully")
                    
        except Exception as e:
            print(f"Error loading models: {str(e)}")
            raise

if __name__ == "__main__":
    recommender = HybridRecommendationSystem(use_combined_model=True)
    
    try:
        try:
            recommender.load_models()
            print("Loaded existing models")
        except FileNotFoundError:
            print("Training new models...")
            recommender.train_models()
            recommender.save_models()
            print("Saved newly trained models")
        
        recommendations = recommender.get_recommendations_for_user(
            "A2SUAM1J3GNN3B",
            category="beauty",
            n=10
        )
        print("\nRecommendations:", recommendations)
        
    except Exception as e:
        print(f"Error: {str(e)}")