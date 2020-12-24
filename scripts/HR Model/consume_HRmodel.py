# -*- coding: utf-8 -*-
"""
Created on Wed Dec 23 22:59:44 2020

@author: nicol
"""


import json
import requests

input_array = [87,88,88,86,85,88,92,92,90,88,91,92,92,92,90,88,88,86,85,85,87,91,95,98]
scoring_uri = "http://b8c05117-db3a-4849-8863-c2313863d0a9.canadacentral.azurecontainer.io/score"

# Add the 'data' field
data = { "data" : input_array, 
        "method" : "predict"} # Write it in the required format for the REST API

input_data = json.dumps(data) # Convert to JSON string

# Set the content type to JSON
headers = {"Content-Type": "application/json"}

# Make the request and display the response
resp = requests.post(scoring_uri, input_data, headers=headers)

# Return the model output
result = json.loads(resp.text)

# 'result' will contain the dictionary: {'predict': 1}