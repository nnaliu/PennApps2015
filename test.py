import urllib2
from zipfile import ZipFile
from StringIO import StringIO
import json
import csv
from pymongo import MongoClient, GEO2D

client = MongoClient()
db = client.crimes
philly_crimes = db.philly
philly_crimes.ensure_index([('loc', GEO2D)])


url = urllib2.urlopen("http://gis.phila.gov/gisdata/police_inct.zip")
zipfile = ZipFile(StringIO(url.read()))

with zipfile.open('police_inct.csv') as csvfile:
	crimes = csv.DictReader(csvfile, delimiter=',')
	for crime in crimes:
		print crime['TEXT_GENERAL_CODE']
		check_duplicate = philly_crimes.find_one({'_id': crime['OBJECTID']})
		if check_duplicate == None and crime['DISPATCH_DATE'] > '2015-07-01' and \
			(crime['POINT_X'] != "" and crime['POINT_Y'] != ""):
			record = {}
			record['_id'] = crime['OBJECTID']
			record['date'] = crime['DISPATCH_DATE']
			record['time'] = crime['DISPATCH_TIME']
			record['address'] = crime['LOCATION_BLOCK']
			record['type'] = crime['TEXT_GENERAL_CODE']
			record['loc'] = [float(crime['POINT_X']), float(crime['POINT_Y'])]

			philly_crimes.insert_one(record)
			print(record)


#url = 'https://api.everyblock.com/content/philly/topnews/?token=2468648eaf3a967061f727a478bd0b703797dc01&schema=crime&date=descending'

#while url != None:
# response = urllib2.urlopen(url)
# data = json.load(response)

# for item in data['results']:

# 	check_duplicate = philly_crimes.find_one({'_id': item['id']})
# 	if check_duplicate == None:
# 		record = {}
# 		record['_id'] = item['id']
# 		record['title'] = item['title']
# 		record['item_date'] = item['item_date']
# 		record['location'] = item['location_name']
# 		record['url'] = item['url']

# 		addressUrl = 'https://maps.googleapis.com/maps/api/geocode/json?address=' \
# 			+ record['location'].replace(' ', '+') + ',+Philadelphia,+PA&key=AIzaSyB9FBh9QtZ1UbwAyn5rr_jXx8dlTk8lONc'
# 		addressResponse = urllib2.urlopen(addressUrl)
# 		addressData = json.load(addressResponse)

# 		addressDataLocation = addressData['results'][0]['geometry']['location']
# 		record['loc'] = [addressDataLocation['lng'], addressDataLocation['lat']]

# 		philly_crimes.insert_one(record)

# 		print(record['lat'])
# 		print
# 	else:
# 		break

#url = data['next']

# for i in all_records:
# 	print(i)

#Google Maps API Key: AIzaSyB9FBh9QtZ1UbwAyn5rr_jXx8dlTk8lONc

