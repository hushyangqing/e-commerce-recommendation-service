import os
import mysql.connector
from dotenv import load_dotenv
load_dotenv()

def update_user_table():
    try:
        service_conn = mysql.connector.connect(
            host=os.getenv('DB_HOST'),
            user=os.getenv('DB_USER'),
            password=os.getenv('DB_PASSWORD'),
            port=os.getenv('DB_PORT'),
            database="service_data"
        )
        cursor = service_conn.cursor()

        try:
            cursor.execute("""
                            SELECT COUNT(*) 
                            FROM information_schema.columns 
                            WHERE table_name = 'users' 
                            AND column_name = 'role'
                            AND table_schema = DATABASE()
                        """)
            
            column_exists = cursor.fetchone()[0]
            
            if not column_exists:
                # Add the column if it doesn't exist
                cursor.execute("""
                    ALTER TABLE users 
                    ADD COLUMN role VARCHAR(20) DEFAULT 'ROLE_USER'
                """)
            
            # Update existing users
            cursor.execute("""
                UPDATE users 
                SET role = 'ROLE_USER' 
                WHERE role IS NULL
            """)
           
            service_conn.commit()
            print("Successfully added role column and updated existing users")
        except mysql.connector.Error as err:
            print(f"Error updating users table: {err}")
            service_conn.rollback()

    except Exception as e:
        print(f"Error: {e}")
        if 'service_conn' in locals():
            service_conn.rollback()
    finally:
        if 'cursor' in locals():
            cursor.close()
        if 'service_conn' in locals():
            service_conn.close()

if __name__ == "__main__":
   update_user_table()