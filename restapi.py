import json
from bottle import route, run, request, abort
from pymongo import MongoClient

client = MongoClient()
db = client.crimes

@route('/near/:latlng', method='GET')
def getCrimes(latlng):
	lat, lng = latlng.split(',')

	entities = db.philly.find({ "loc" : { "$within" : { "$center": [[float(lng), float(lat)], (20.0 / 3959)] } } })

	results = [doc for doc in entities]

	return json.dumps(results)

run(host='localhost', port=8080)