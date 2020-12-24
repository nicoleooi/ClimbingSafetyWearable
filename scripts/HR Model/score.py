import os
import json
import numpy as np
import joblib

from azureml.core import Run
from azureml.core.model import Model

from sklearn.preprocessing import MinMaxScaler

def init():
    """
    Scoring Script
    """
    
    global model, run
    
    model_artifact = "100-0.0004-0.0002-0.0094-0.0094.hdf5"
    model_path = os.path.join(os.getenv('AZUREML_MODEL_DIR'), model_artifact)
    #model is of type Sequential() from keras
    model = joblib.load(model_path)
    
def run(data):
    """
    Predictions
    """
    input_data = np.array(json.loads(data)['data'])
    
    #Normalize data using min/max
    scaler = MinMaxScaler(feature_range=(0,1))
    input_data = scaler.fit_transform(input_data)
    
    #Reformat into 3D
    input_data = np.reshape(input_data, (1, input_data.shape[0], 1)) #1 sample, time stamp is 0, then 1 feature

    #Predict output
    output = model.predict(input_data)
    output = scaler.inverse_transform(output)
    
    result = {"predictedHR" : output.tolist()}

    return result