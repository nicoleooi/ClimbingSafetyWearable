import os
import json
import numpy as np
import joblib

from azureml.core import Run
from azureml.core.model import Model

from sklearn.preprocessing import MinMaxScaler
from keras.models import Sequential
from keras.layers import Dense, Activation, CuDNNLSTM, LSTM

def init():
    """
    Scoring Script
    """
    
    global model, run
    
    model_artifact = "hrpredictor.joblib"
    model_path = os.path.join(os.getenv('AZUREML_MODEL_DIR'), model_artifact)
    
    model = joblib.load(model_path)
    '''
    #model is of type Sequential() from keras
    model = Sequential()
    #layer 1 = LSTM w 50 neurons
    model.add(LSTM(50, return_sequences = True, input_shape = (24, 1)))
    #layer 2 = LSTM w 50 neurons
    model.add(LSTM(50, return_sequences = False))
    #fully connected layer
    model.add(Dense(50, activation='relu'))
    #output layer (single output)
    model.add(Dense(1))
    model.load_weights(model_path, compile=False)
    '''
    
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