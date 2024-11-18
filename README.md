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

