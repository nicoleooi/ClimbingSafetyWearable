# -*- coding: utf-8 -*-
"""
Created on Wed Dec 23 22:59:44 2020

@author: nicol
"""


import json
import requests
import numpy as np

input_array = [[1.038291243,    -45.00022028,   0.038669902,    -0.519148163,   -0.019335046], 
               [1.0548177,      -45.0075678,    0.038472101,    -0.527497546,   -0.019239285],
               [1.078606097,    -45.01275791,	0.030757843,	-0.539455946,	-0.015383282],
               [1.092423861,    -45.01555917,	0.030757843,	-0.546400788,	-0.015384239],
               [1.104315598,	-45.00470017,	0.019918045,	-0.552215471,	-0.009960063],
               [1.127878533,	-45.00018012,	0.028437929,	-0.563941524,	-0.014219022],
               [1.110921293,	-45.01522295,	0.045554312,	-0.555648552,	-0.022784861],
               [1.055880402,	-45.04084103,	0.059241214,	-0.528419348,	-0.02964749],
               [1.086731106,	-45.00948369,	0.05496581,     -0.543480067,	-0.027488697],
               [1.083947423,	-45.03817694,	0.027621359,	-0.542433509,	-0.013822396],
               [1.124911835,	-45.07865654,	0.064659943,	-0.563439047,	-0.032386482],
               [1.133095331,	-45.00017635,	0.080907481,	-0.566549886,	-0.040453899],
               [1.124687999,   	-45,        	0.009568319,   	-0.562344,      -0.00478416],
               [1.107268577,	-45.00165604,	0.022777156,	-0.553654663,	-0.011388997],
               [1.090641509,	-45.02289999,	0.036851489,	-0.545598262,	-0.018435121]
               ]
adl_scoring_url = "http://700a2601-5dee-4509-8cd5-8a83683deb76.eastus2.azurecontainer.io/score"
fall_scoring_url = "http://e7697dac-5e32-4b3d-bf31-12d0779da73c.eastus2.azurecontainer.io/score"
# Add the 'data' field
data = { "data" : input_array, 
        "method" : "predict"} # Write it in the required format for the REST API

input_data = json.dumps(data) # Convert to JSON string

# Set the content type to JSON
headers = {"Content-Type": "application/json"}

# Make the request and display the response
adl_resp = requests.post(adl_scoring_url, input_data, headers=headers)
fall_resp = requests.post(fall_scoring_url, input_data, headers=headers)
# Return the model output
adl_result = json.loads(adl_resp.text)
fall_result = json.loads(fall_resp.text)
adl_score = adl_result['ADL-prob']
fall_score = fall_result['FALL-prob']

#imbalanced thresholding here
if (fall_score < 0) and (adl_score < 0):
    fall_score2 = np.exp(fall_score)
    adl_score2 = np.exp(adl_score)
    percent_fall = (fall_score2)/(fall_score2 + adl_score2)
    if(percent_fall >= 0.55):
        prediction = 1
    else:
        prediction = 0
elif (fall_score < 0) and (adl_score > 0): #if one's neg
    prediction = 0
elif (fall_score > 0) and (adl_score < 0): #if ones neg
    prediction = 1
else:
    percent_fall = (fall_score)/(fall_score + adl_score)
    if(percent_fall >= 0.55):
        prediction = 1
    else:
        prediction = 0
        
print(prediction)
# 'result' will contain the dictionary: {'predict': 1}