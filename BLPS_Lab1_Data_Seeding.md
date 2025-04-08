# BLPS Lab1 Database Seeding Guide

This guide will help you populate your database with test data using your API endpoints.

## Setup Instructions

1. Import the `BLPS_Lab1_Postman_Collection.json` and `BLPS_Lab1_Postman_Environment.json` files into Postman
2. Select the `BLPS Lab1 Environment` environment from the dropdown in the top-right corner
3. Follow the steps below in sequence to populate your database

## Step 1: Create Users

### Register User 1
1. Open the "Register" request in the "Authentication" folder
2. Replace the request body with:
```json
{
  "username": "user1",
  "password": "password123"
}
```
3. Send the request
4. Save the token from the response: `pm.environment.set("auth_token", responseBody.token)`

### Register User 2
1. Open the "Register" request again
2. Replace the request body with:
```json
{
  "username": "user2",
  "password": "password123"
}
```
3. Send the request

### Register User 3 (Admin)
1. Open the "Register" request again
2. Replace the request body with:
```json
{
  "username": "admin",
  "password": "admin123456"
}
```
3. Send the request

## Step 2: Create Promotions

1. Login as admin:
```json
{
  "username": "admin",
  "password": "admin123456"
}
```

### Create Basic Promotion
1. Open the "Create Promotion" request in the "Promotions" folder
2. Replace the request body with:
```json
{
  "name": "Basic Visibility",
  "description": "Your property will be highlighted in search results for 7 days",
  "price": 50.0
}
```
3. Send the request
4. Save the promotion ID from the response: `pm.environment.set("promotion1_id", responseBody.id)`

### Create Premium Promotion
1. Open the "Create Promotion" request again
2. Replace the request body with:
```json
{
  "name": "Premium Listing",
  "description": "Your property will appear at the top of search results for 14 days",
  "price": 100.0
}
```
3. Send the request
4. Save the promotion ID from the response: `pm.environment.set("promotion2_id", responseBody.id)`

### Create Featured Promotion
1. Open the "Create Promotion" request again
2. Replace the request body with:
```json
{
  "name": "Featured Property",
  "description": "Your property will be featured on the homepage and highlighted in search results for 30 days",
  "price": 200.0
}
```
3. Send the request
4. Save the promotion ID from the response: `pm.environment.set("promotion3_id", responseBody.id)`

## Step 3: Create Payment Providers

Since there's no direct endpoint for creating payment providers, you would typically add them via SQL directly. However, let's assume they already exist and the "Get Available Payment Providers" endpoint will return them.

## Step 4: Create Points of Interest (POIs)

### Create School POI
1. Open the "Add POI" request in the "Points of Interest (POI)" folder
2. Replace the request body with:
```json
{
  "name": "Central School",
  "address": "123 Education Street",
  "city": "Saint Petersburg",
  "type": "SCHOOL"
}
```
3. Send the request
4. Save the POI ID from the response if available: `pm.environment.set("poi1_id", responseBody.id)`

### Create Metro Station POI
1. Open the "Add POI" request again
2. Replace the request body with:
```json
{
  "name": "Main Station",
  "address": "456 Transit Avenue",
  "city": "Saint Petersburg",
  "type": "METRO_STATION"
}
```
3. Send the request
4. Save the POI ID from the response if available: `pm.environment.set("poi2_id", responseBody.id)`

### Create Park POI
1. Open the "Add POI" request again
2. Replace the request body with:
```json
{
  "name": "Riverside Park",
  "address": "789 Nature Boulevard",
  "city": "Saint Petersburg",
  "type": "PARK"
}
```
3. Send the request

### Create Supermarket POI
1. Open the "Add POI" request again
2. Replace the request body with:
```json
{
  "name": "City Supermarket",
  "address": "101 Shopping Street",
  "city": "Saint Petersburg",
  "type": "SUPERMARKET"
}
```
3. Send the request

## Step 5: Create Advertisements

Login as user1:
```json
{
  "username": "user1",
  "password": "password123"
}
```

### Create Apartment Advertisement
1. Open the "Create Advertisement" request in the "Advertisements" folder
2. Replace the request body with:
```json
{
  "title": "Modern Apartment in City Center",
  "description": "Beautiful 2-bedroom apartment with a spacious living room, fully equipped kitchen, and a balcony with a city view. Walking distance to shops and public transport.",
  "price": 120000,
  "address": "123 Central Avenue, Apt 5B",
  "city": "Saint Petersburg",
  "realEstateType": "APARTMENT"
}
```
3. Send the request
4. Save the advertisement ID from the response: `pm.environment.set("advertisement1_id", responseBody.id)`

### Create House Advertisement
1. Open the "Create Advertisement" request again
2. Replace the request body with:
```json
{
  "title": "Spacious Family House with Garden",
  "description": "Lovely 4-bedroom house with a large garden, garage, and modern amenities. Perfect for families. Located in a quiet neighborhood with good schools nearby.",
  "price": 250000,
  "address": "456 Suburban Road",
  "city": "Saint Petersburg",
  "realEstateType": "HOUSE"
}
```
3. Send the request
4. Save the advertisement ID from the response: `pm.environment.set("advertisement2_id", responseBody.id)`

Login as user2:
```json
{
  "username": "user2",
  "password": "password123"
}
```

### Create Commercial Property Advertisement
1. Open the "Create Advertisement" request again
2. Replace the request body with:
```json
{
  "title": "Prime Commercial Space in Business District",
  "description": "Excellent 150 sq.m. commercial space in the heart of the business district. Perfect for offices or retail. Modern building with all amenities and 24/7 security.",
  "price": 350000,
  "address": "789 Business Avenue",
  "city": "Saint Petersburg",
  "realEstateType": "COMMERCIAL"
}
```
3. Send the request
4. Save the advertisement ID from the response: `pm.environment.set("advertisement3_id", responseBody.id)`

### Create Land Advertisement
1. Open the "Create Advertisement" request again
2. Replace the request body with:
```json
{
  "title": "Development Land with Great Potential",
  "description": "1000 sq.m. of land with planning permission for residential development. Great investment opportunity in a developing area with excellent transport links.",
  "price": 180000,
  "address": "101 Development Road",
  "city": "Moscow",
  "realEstateType": "LAND"
}
```
3. Send the request

## Step 6: Apply Promotions to Advertisements

Login as user1:
```json
{
  "username": "user1",
  "password": "password123"
}
```

### Apply Basic Promotion to Apartment
1. Open the "Add Promotion to Advertisement" request
2. Update the URL to use the apartment advertisement ID: `{{base_url}}/advertisements/{{advertisement1_id}}/promotion`
3. Replace the request body with (use the actual promotion1_id):
```json
{
  "id": {{promotion1_id}},
  "name": "Basic Visibility",
  "description": "Your property will be highlighted in search results for 7 days",
  "price": 50.0,
  "active": true
}
```
4. Send the request

Login as user2:
```json
{
  "username": "user2",
  "password": "password123"
}
```

### Apply Premium Promotion to Commercial Property
1. Open the "Add Promotion to Advertisement" request
2. Update the URL to use the commercial property advertisement ID: `{{base_url}}/advertisements/{{advertisement3_id}}/promotion`
3. Replace the request body with (use the actual promotion2_id):
```json
{
  "id": {{promotion2_id}},
  "name": "Premium Listing",
  "description": "Your property will appear at the top of search results for 14 days",
  "price": 100.0,
  "active": true
}
```
4. Send the request

## Step 7: Update Advertisement POIs

### Update POIs for Apartment Advertisement
1. Open the "Update Advertisement POIs" request
2. Update the URL to use the apartment advertisement ID: `{{base_url}}/poi/update-advertisement-pois/{{advertisement1_id}}`
3. Send the request

### Update POIs for House Advertisement
1. Open the "Update Advertisement POIs" request
2. Update the URL to use the house advertisement ID: `{{base_url}}/poi/update-advertisement-pois/{{advertisement2_id}}`
3. Send the request

## Step 8: Process Payments

Login as user1:
```json
{
  "username": "user1",
  "password": "password123"
}
```

1. First, get available payment providers:
   - Open the "Get Available Payment Providers" request
   - Send the request
   - Note the ID of the first provider: `pm.environment.set("payment_provider1_id", responseBody[0].id)`

2. Process payment for the apartment promotion:
   - Open the "Process Payment" request
   - Replace the request body with (use actual IDs):
```json
{
  "promotionId": {{promotion1_id}},
  "providerId": {{payment_provider1_id}},
  "amount": 50.0
}
```
   - Send the request

Login as user2:
```json
{
  "username": "user2",
  "password": "password123"
}
```

3. Process payment for the commercial property promotion:
   - Open the "Process Payment" request
   - Replace the request body with (use actual IDs):
```json
{
  "promotionId": {{promotion2_id}},
  "providerId": {{payment_provider1_id}},
  "amount": 100.0
}
```
   - Send the request

## Step 9: Test Location-based Searches

1. Find advertisements in Saint Petersburg:
   - Open the "Find Advertisements by City" request
   - Update the URL to: `{{base_url}}/locations/city/Saint%20Petersburg`
   - Send the request

2. Find nearby advertisements:
   - Open the "Find Nearby Advertisements" request
   - Update the query parameters to use the coordinates of Saint Petersburg: 
     - latitude: 59.9343
     - longitude: 30.3351
   - Send the request

## Note for Database Initialization

If you need to initialize your database with some data that cannot be added via the API (like payment providers), you might need to run SQL scripts directly. Here's a sample SQL script for adding payment providers:

```sql
INSERT INTO payment_provider (name) VALUES ('Credit Card');
INSERT INTO payment_provider (name) VALUES ('PayPal');
INSERT INTO payment_provider (name) VALUES ('Bank Transfer');
```

You could run this script directly on your database if needed.

## Verification

After completing all these steps, you should have a database populated with:
- 3 users
- 3 promotions
- 4+ points of interest
- 4+ advertisements 
- 2 advertisements with promotions
- 2 processed payments

You can verify this by using the "Get All" endpoints for each entity type. 