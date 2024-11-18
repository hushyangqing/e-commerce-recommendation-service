import json
import os
from pathlib import Path
import mysql.connector
from multiprocessing import Process, current_process
import logging
import pdb
from dotenv import load_dotenv

logging.basicConfig(level=logging.DEBUG)
load_dotenv()

review_base_path = 'data/review'
meta_base_path = 'data/meta'

def readFile(file):
    with open(file, 'r') as fp:
        for line in fp:
            print(json.loads(line.strip()))

def filename_to_table_name(filename):
    return os.path.splitext(os.path.basename(filename))[0].replace('.', '_').replace('-', '_')

def safe_float(value):
    try:
        return float(value)
    except (TypeError, ValueError):
        return None
    
# Function to create db
def create_database():
    try:
        connection = mysql.connector.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            database=os.getenv('DB_NAME'),
            port=os.getenv('DB_PORT')
        )
        cursor = connection.cursor()

        try:
            cursor.execute("USE amazon_reviews")
        except mysql.connector.Error as err:
            if err.errno == mysql.connector.errorcode.ER_BAD_DB_ERROR:
                print("Database 'amazon_reviews' does not exist. Creating database...")
                cursor.execute("CREATE DATABASE amazon_reviews")
                print("Database 'amazon_reviews' created successfully.")
            else:
                print(f"Failed to connect to database: {err}")
                return

        # Close the connection
        cursor.close()
        connection.close()

    except mysql.connector.Error as err:
        print(f"Error: {err}")

# Function to insert review data into MySQL
def insert_review_data_into_mysql(file, table_name, batch_size=1000):
    try:
        connection = mysql.connector.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            database=os.getenv('DB_NAME'),
            port=os.getenv('DB_PORT'),
            connection_timeout=120
        )
        cursor = connection.cursor()

        connection.autocommit = False

        create_table_query = f'''
        CREATE TABLE IF NOT EXISTS {table_name} (
            id INT AUTO_INCREMENT PRIMARY KEY,
            rating FLOAT,
            title TEXT,
            text TEXT,
            images JSON,
            asin VARCHAR(50),
            parent_asin VARCHAR(50),
            user_id VARCHAR(100),
            timestamp BIGINT,
            verified_purchase BOOLEAN,
            helpful_vote INT
        );
        '''
        cursor.execute(create_table_query)

        full_file_path = os.path.join(review_base_path, file)

        batch_data = []
        with open(full_file_path, 'r') as fp:
            for line in fp:
                data = json.loads(line.strip())
                rating = data.get('rating')
                title = data.get('title')
                text = data.get('text')
                images = json.dumps(data.get('images'))
                asin = data.get('asin')
                parent_asin = data.get('parent_asin')
                user_id = data.get('user_id')
                timestamp = data.get('timestamp')
                verified_purchase = data.get('verified_purchase')
                helpful_vote = data.get('helpful_vote')

                batch_data.append((rating, title, text, images, asin, parent_asin, user_id, timestamp, verified_purchase, helpful_vote))

                if len(batch_data) >= batch_size:
                    cursor.executemany(f'''
                    INSERT INTO {table_name} 
                    (rating, title, text, images, asin, parent_asin, user_id, timestamp, verified_purchase, helpful_vote)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
                    ''', batch_data)
                    connection.commit()
                    batch_data = [] 

        if batch_data:
            cursor.executemany(f'''
            INSERT INTO {table_name} 
            (rating, title, text, images, asin, parent_asin, user_id, timestamp, verified_purchase, helpful_vote)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
            ''', batch_data)
            connection.commit()

        cursor.close()
        connection.close()
        print(f"Review data inserted into {table_name} successfully.")
    
    except Exception as e:
        print(f"Error in {current_process().name} for review table {table_name}: {e}")

# Function to insert metadata into MySQL
def insert_metadata_into_mysql(file, table_name, batch_size=1000):
    try:
        connection = mysql.connector.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            database=os.getenv('DB_NAME'),
            port=os.getenv('DB_PORT'),
            connection_timeout=120
        )
        cursor = connection.cursor()

        connection.autocommit = False

        create_table_query = f'''
        CREATE TABLE IF NOT EXISTS {table_name} (
            id INT AUTO_INCREMENT PRIMARY KEY,
            main_category VARCHAR(255),
            title TEXT,
            average_rating FLOAT,
            rating_number INT,
            features JSON,
            description JSON,
            price FLOAT,
            images JSON,
            videos JSON,
            store TEXT,
            categories JSON,
            details JSON,
            parent_asin VARCHAR(50),
            bought_together JSON
        );
        '''
        cursor.execute(create_table_query)

        full_file_path = os.path.join(meta_base_path, file)

        batch_data = []

        with open(full_file_path, 'r') as fp:
            for line in fp:
                data = json.loads(line.strip())
                main_category = data.get('main_category')
                title = data.get('title')
                average_rating = data.get('average_rating')
                rating_number = data.get('rating_number')
                features = json.dumps(data.get('features'))
                description = json.dumps(data.get('description'))
                price = safe_float(data.get('price'))
                images = json.dumps(data.get('images'))
                videos = json.dumps(data.get('videos'))
                store = data.get('store')
                categories = json.dumps(data.get('categories'))
                details = json.dumps(data.get('details'))
                parent_asin = data.get('parent_asin')
                bought_together = json.dumps(data.get('bought_together'))

                batch_data.append((main_category, title, average_rating, rating_number, features, description, price, images, videos, store, categories, details, parent_asin, bought_together))

                if len(batch_data) >= batch_size:
                    cursor.executemany(f'''
                    INSERT INTO {table_name} 
                    (main_category, title, average_rating, rating_number, features, description, price, images, videos, store, categories, details, parent_asin, bought_together)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
                    ''', batch_data)
                    connection.commit()
                    batch_data = [] 

        if batch_data:
            cursor.executemany(f'''
            INSERT INTO {table_name} 
            (main_category, title, average_rating, rating_number, features, description, price, images, videos, store, categories, details, parent_asin, bought_together)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
            ''', batch_data)
            connection.commit()

        cursor.close()
        connection.close()
        print(f"Metadata inserted into {table_name} successfully.")

    except mysql.connector.Error as err:
        print(f"Error in inserting data into {table_name}: {err}")

# Function to set up multiprocessing for both review and metadata
def process_files_in_parallel(file_table_pairs, data_type='review'):
    processes = []
    
    for file, table_name in file_table_pairs:
        if data_type == 'review':
            p = Process(target=insert_review_data_into_mysql, args=(file, table_name))
        elif data_type == 'meta':
            p = Process(target=insert_metadata_into_mysql, args=(file, table_name))
        processes.append(p)
        p.start()
    
    for p in processes:
        p.join()

if __name__ == '__main__':
    review_files = [f for f in os.listdir('data/review') if f.endswith('.jsonl')]
    meta_files = [f for f in os.listdir('data/meta') if f.endswith('.jsonl')]
    print(review_files)
    print(meta_files)

    review_file_table_pairs = [(file, filename_to_table_name(file)) for file in review_files]
    meta_file_table_pairs = [(file, filename_to_table_name(file)) for file in meta_files]

    # Run the processes for review and metadata files
    process_files_in_parallel(review_file_table_pairs, data_type='review')
    process_files_in_parallel(meta_file_table_pairs, data_type='meta')

