# e-commerce-recommendation-service
Spring Boot Recommendation Microservice

Please notice this implementation is focused on the backend part instead of the recommendation algorithm although it's used somewhere. Spring Boot, Security, Unit Test, Load Test, CI/CD Jenkins, Kubernetes, and Cloud deployment are included.

### Data Source
This project utilizes data from [Amazon Review Data (2023)](https://amazon-reviews-2023.github.io/). 
Please refer to the original data source for further details.
Our subset includes 5 categories for simplicity: All_Beauty, Amazon_Fashion, Appliances, Arts_Crafts_and_Sewing, Automotive. (24.43 GB in total)

#### Write data into mysql db
python -m venv venv

source venv/bin/activate 

pip install -r requirements.txt

python process.py


### Recommendation Model(offline)
Python3.9 used since python3.11 can not install surprise. Besides, we sample data here since the recommendation model is not our main focus.

python3.9 -m venv myenv39 

source myenv39/bin/activate

python3.9 -m pip install mysql-connector-python pandas scikit-surprise scikit-learn python-dotenv

python3.9 -m pip install joblib tqdm psutil pyarrow

python3.9 memory_efficient_training.py # to include more recommendations for users, change cursor.execute("SELECT user_id FROM users LIMIT 1000") in def def save_recommendations(self):

python3.9 update_user.py
### Spring Boot API
All functions are tested by Unit Test.
Wait for Load Test and Automatic Test


