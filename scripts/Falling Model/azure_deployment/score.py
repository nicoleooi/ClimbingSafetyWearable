import os
import json
import numpy as np
import joblib

from azureml.core import Run
from azureml.core.model import Model

from sklearn.preprocessing import MinMaxScaler
from keras.models import Sequential
from keras.layers import Dense, Activation, CuDNNLSTM, LSTM
from hmmlearn.hmm import GaussianHMM

def init():
    """
    Scoring Script
    """
    
    global model, run
    
    model_artifact = "model_v2_100.joblib"
    model_path = os.path.join(os.getenv('AZUREML_MODEL_DIR'), model_artifact)
    
    model = joblib.load(model_path)
    
def run(data):
    """
    Predictions
    """
    input_data = np.array(json.loads(data)['data'])

    #Predict output
    output = model.score(input_data)
    
    result = {"ADL-prob" : output.tolist()}

    return result