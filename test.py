import urllib2
import json
from pymongo import MongoClient

client = MongoClient()
db = client.crimes
philly_crimes = db.philly
philly_crimes.ensure_index([('loc', '2d')])

url = 'https://api.everyblock.com/content/philly/topnews/?token=2468648eaf3a967061f727a478bd0b703797dc01&schema=crime&date=descending'

#while url != None:
response = urllib2.urlopen(url)
data = json.load(response)

for item in data['results']:

	check_duplicate = philly_crimes.find_one({'_id': item['id']})
	if check_duplicate == None:
		record = {}
		record['_id'] = item['id']
		record['title'] = item['title']
		record['item_date'] = item['item_date']
		record['location'] = item['location_name']
		record['url'] = item['url']

		addressUrl = 'https://maps.googleapis.com/maps/api/geocode/json?address=' \
			+ record['location'].replace(' ', '+') + ',+Philadelphia,+PA&key=AIzaSyB9FBh9QtZ1UbwAyn5rr_jXx8dlTk8lONc'
		addressResponse = urllib2.urlopen(addressUrl)
		addressData = json.load(addressResponse)

		addressDataLocation = addressData['results'][0]['geometry']['location']
		record['loc'] = [addressDataLocation['lng'], addressDataLocation['lat']]

		philly_crimes.insert_one(record)

		#print(record['lat'])
		#print
	else:
		break

#url = data['next']

# for i in all_records:
# 	print(i)

#Google Maps API Key: AIzaSyB9FBh9QtZ1UbwAyn5rr_jXx8dlTk8lONc

