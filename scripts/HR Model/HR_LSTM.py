# -*- coding: utf-8 -*-
"""
Created on Mon Nov 16 22:45:18 2020

@author: nicol

Univariate sequence prediction for HR (multi-step time series forecasting)

activation function: tanh() 

step 1: forget gate - sigmoid layer. decides which info to discard from the cell
step 2: input gate - decides input values to update memory with
step 3: forget + input gate - update cell state using forget and input and tanh
step 4: output gate - decides what to output based on input and long term mem

need to normalize data (sklearn)
sequence padding to pad 0s to the start (keras)
input must be 3D in first layer (samples, time steps, features)


1. collect and pre-process data
2. split into training, valid, test data
3. data normalization
4. create network architecture
5. set hyperparams (learning rate, batch size, num epochs)
6. train network
7. evaluate

"""
import os
import glob
import pandas as pd
import numpy as np
import keras
import time
import math
import joblib
import tensorflow as tf

from keras.models import Sequential
from keras.layers import Dense, Activation, CuDNNLSTM, LSTM
#dense is for output layer
#CuDNNLSTM is if we can use a GPU
from keras import optimizers
from sklearn.preprocessing import MinMaxScaler
from LSTM_animate import graph

N_TRAIN = 11708

def format_data(person, version, n_in, n_out):
    '''
    Formats data and returns the formatted arrays.
    '''
    print("Formatting data...")
    
    path = "../../converted_data/hr_only/"+person+"*.csv" 
    appended_data = []

    for f in glob.glob(path):
        df = pd.read_csv(f, parse_dates=True,header = 0)
        appended_data.append(df)
        
    df = pd.concat(appended_data)
    #df.to_csv('appended.csv')

    df['Date Time'] = pd.to_datetime(df['Time'])
    df['Hour'] = df['Date Time'].dt.hour 
    df['Minute'] = df['Date Time'].dt.minute  
    df['Second'] = df['Date Time'].dt.second

    df['HR'] = pd.to_numeric(df['HR'], errors = 'coerce') 
    df.drop(['Date Time'], axis = 1, inplace = True)
    df.drop(['Time'], axis = 1, inplace = True)

    #rearrange so HR is the last column
    df = df[['Hour', 'Minute', 'Second', 'HR']]
    
    '''
    x = [] #features
    y = [] #labels 
    for i in range(0, df.shape[0]-48):
        x.append(df.iloc[i:i+48, 3])
        y.append(df.iloc[i+48:i+48+(num_predictions-1), 3])

    x, y = np.array(x), np.array(y)
    y = np.reshape(y, (len(y), num_predictions))
    
    
    x = np.delete(x, list(range(1, x.shape[1], 2)), axis=1)
    x = np.delete(x, list(range(1, x.shape[0], 2)), axis=0) 
    y = np.delete(y, list(range(1, y.shape[0], 2)), axis=0)
    '''
    
    def to_supervised(df, n_input, n_output):
        x = []
        y = []
        for i in range(df.shape[0]): #moving by 1, could move by n_input
            in_end = i + n_input
            out_end = in_end + n_output
            if out_end <= df.shape[0]:
                x_input = np.array(df.iloc[i:in_end, 3])
                y_input= np.array(df.iloc[in_end:out_end,3])
                #x_input = x_input.reshape((len(x_input), 1))
                x.append(x_input)
                y.append(y_input)
            
        x, y = np.array(x), np.array(y)
        
        return x,y
            
    x,y = to_supervised(df, n_in, n_out)
    pd.DataFrame(x).to_csv('./formatted_data/dropped_HR_x_v'+version+'.csv')
    pd.DataFrame(y).to_csv('./formatted_data/dropped_HR_y_v'+version+'.csv')

    '''Data Normalization'''

    scaler = MinMaxScaler(feature_range=(0,1))

    x = scaler.fit_transform(x)
    y = scaler.fit_transform(y)

    #train on 80% of data
    split = math.ceil(x.shape[0] * 0.8)
    N_TRAIN = split
    x_train, x_test = x[:-split], x[-split:]
    y_train, y_test = y[:-split], y[-split:]

    # num samples, num time stamps, num features
    x_train = np.reshape(x_train, (x_train.shape[0], x_train.shape[1], 1))
    x_test = np.reshape(x_test, (x_test.shape[0], x_test.shape[1], 1))
    
    print("Finished formatting data")

    return x_train, x_test, y_train, y_test

def normalize_data(version):
    '''Data Normalization'''
    print("Starting data normalization...")
    
    x = pd.read_csv('dropped_HR_x_v'+version+'.csv', header = 0)
    x.drop(x.columns[0],inplace=True, axis=1)
    y = pd.read_csv('dropped_HR_y_v'+version+'.csv', header = 0)
    y.drop(y.columns[0], inplace=True, axis=1)

    scaler = MinMaxScaler(feature_range=(0,1))

    x = scaler.fit_transform(x)
    y = scaler.fit_transform(y)
 
    split = math.ceil(x.shape[0] * 0.8)
    x_train, x_test = x[:-split], x[-split:]
    y_train, y_test = y[:-split], y[-split:]

    # num samples, num time stamps, num features
    x_train = np.reshape(x_train, (x_train.shape[0], x_train.shape[1], 1))
    x_test = np.reshape(x_test, (x_test.shape[0], x_test.shape[1], 1))
    
    print("Finished data normalization")
    
    return x_train, x_test, y_train, y_test, scaler
    
def train_model(model, x_train, x_test, y_train, y_test, version):
    '''Model checks and tools to stop overfitting'''
    print("Training model")
    from keras.callbacks import ModelCheckpoint, EarlyStopping
    filepath = 'models_v'+version+'/{epoch:02d}-{loss:.4f}-{val_loss:.4f}-{mse:.4f}-{mae:.4f}.hdf5'
    
    savedModel = ModelCheckpoint(filepath, monitor='val_loss', save_best_only=True, mode='min')
    earlyStopper = EarlyStopping(monitor = 'val_loss', patience = 50)
    tbCallBack = keras.callbacks.TensorBoard(log_dir='./models_v'+version+'/tblogs' , histogram_freq=0, write_graph=True, write_images=False)
    cbs = [savedModel, earlyStopper, tbCallBack]
    
    nu = 0.0001 #learning rate
    optimizers.Adam(lr=nu)
        
    model.compile(optimizer='adam', loss='mse', metrics=['mse','mae'])
    model.fit(x_train, y_train, validation_split=0.2, epochs=100, callbacks=cbs, batch_size=8)
    #num epochs = number of passes of training
    #batch size = number of samples to work through before updating internal params
    return model

def test_model(model, scaler, x_train, x_test, y_train, y_test, version):
    print("Starting testing...")
    
    #the best model will be the LAST file in the /models folder
    path="./models_v"+version+"/"
    files = sorted(os.listdir(path), key=lambda x: int(''.join(filter(str.isdigit, x[0:3]))))
    best_model = files[-1]
    print(best_model)
    model.load_weights("./models_v"+version+"/"+best_model)
    joblib.dump(model, './best_models/hrpredictor_v'+version+'.joblib')

    for i in range(0, x_test.shape[0]):
        hr_summary = [] #forecasted hr
        x_input = x_test[i, :, :]
        x_input = np.reshape(x_input, (1, x_input.shape[0], 1)) #1 sample, time stamp is 0, then 1 feature
        prediction = model.predict(x_input)
        prediction = scaler.inverse_transform(prediction)
        
        y_input = y_test[i,:]
        y_input = np.reshape(y_input, (1,1))
        actual = scaler.inverse_transform(y_input)
        
        hr_summary.append(actual)
        hr_summary.extend(prediction)
        
        df = pd.DataFrame(hr_summary)
        df = df.T #put it into a single row
        df.to_csv("real_time_HR_v"+version+".csv", mode='a', header=False, index=False)
        
        #period of prediction, if you want real time don't sleep
        #time.sleep(0.5)
        #print(df)
        
    print("Finished testing")
    return

def main_v1():
    x_train, x_test, y_train, y_test, scaler = normalize_data()
    
    '''Network Architecture'''
    model = Sequential()
    #layer 1 = LSTM w 50 neurons
    model.add(LSTM(50, return_sequences = True, input_shape = (x_train.shape[1], 1)))
    #layer 2 = LSTM w 50 neurons
    model.add(LSTM(50, return_sequences = False))
    #fully connected layer
    model.add(Dense(50, activation='relu'))
    #output layer (single output)
    model.add(Dense(1))

    test_model(model, scaler, x_train, x_test, y_train, y_test)
    
def main_v2():
    '''
    Version 2.0: Trained on ALL available data
    '''
    '''
    #Format Data
    x_train, x_test, y_train, y_test = format_data("","2.0")
    '''
    #Normalize Data
    x_train, x_test, y_train, y_test, scaler = normalize_data("2.0")
    
    #Network Architecture
    model = Sequential()
    #layer 1 = LSTM w 50 neurons
    model.add(LSTM(50, return_sequences = True, input_shape = (x_train.shape[1], 1)))
    #layer 2 = LSTM w 50 neurons
    model.add(LSTM(50, return_sequences = False))
    #fully connected layer
    model.add(Dense(50, activation='relu'))
    #output layer (single output)
    model.add(Dense(1))
    
    '''
    #Train Model
    model = train_model(model, x_train, x_test, y_train, y_test, "2.0")
    '''
    
    #Test Model
    #test_model(model, scaler, x_train, x_test, y_train, y_test, "2.0")
    
def main():
    '''
    Version 3.0: Changed architecture
    '''
    n_in = 15
    n_out = 30
    
    #Format Data
    x_train, x_test, y_train, y_test = format_data("","3.0", n_in, n_out)
    
    '''
    #Normalize Data
    x_train, x_test, y_train, y_test, scaler = normalize_data("3.0")
    '''
    
    #Network Architecture
    model = Sequential()
    #layer 1 = LSTM w 50 neurons
    model.add(LSTM(100, return_sequences = True, input_shape = (x_train.shape[1], 1)))
    #layer 2 = LSTM w 50 neurons
    model.add(LSTM(100, return_sequences = False))
    #fully connected layer
    model.add(Dense(100, activation='relu'))
    #output layer (30 predictions)
    model.add(Dense(n_out))
    
    
    #Train Model
    model = train_model(model, x_train, x_test, y_train, y_test, "3.0")
    
    
    #Test Model
    #test_model(model, scaler, x_train, x_test, y_train, y_test, "2.0")

def main_v4():
    '''
    Version 4.0: Changed architecture
    '''
    n_in = 15
    n_out = 45
    
    #Format Data
    x_train, x_test, y_train, y_test = format_data("","4.0", n_in, n_out)
    
    '''
    #Normalize Data
    x_train, x_test, y_train, y_test, scaler = normalize_data("3.0")
    '''
    
    #Network Architecture
    model = Sequential()
    #layer 1 = LSTM w 50 neurons
    model.add(LSTM(100, return_sequences = True, input_shape = (x_train.shape[1], 1)))
    #layer 2 = LSTM w 50 neurons
    model.add(LSTM(100, return_sequences = False))
    #fully connected layer
    model.add(Dense(100, activation='relu'))
    #output layer (30 predictions)
    model.add(Dense(n_out))
    
    
    #Train Model
    model = train_model(model, x_train, x_test, y_train, y_test, "4.0")
    
def main_v5():
    '''
    Version 5.0: Changed architecture
    TODO: Alex run
    '''
    n_in = 15
    n_out = 60
    
    #Format Data
    x_train, x_test, y_train, y_test = format_data("","5.0", n_in, n_out)
    
    '''
    #Normalize Data
    x_train, x_test, y_train, y_test, scaler = normalize_data("3.0")
    '''
    
    #Network Architecture
    model = Sequential()
    #layer 1 = LSTM w 50 neurons
    model.add(LSTM(100, return_sequences = True, input_shape = (x_train.shape[1], 1)))
    #layer 2 = LSTM w 50 neurons
    model.add(LSTM(100, return_sequences = False))
    #fully connected layer
    model.add(Dense(100, activation='relu'))
    #output layer (30 predictions)
    model.add(Dense(n_out))
    
    
    #Train Model
    model = train_model(model, x_train, x_test, y_train, y_test, "5.0")
    
def main_v6():
    '''
    Regularizing v3.0
    '''
    n_in = 15
    n_out = 45
    
    #Format Data
    x_train, x_test, y_train, y_test = format_data("","6.0", n_in, n_out)
    
    '''
    #Normalize Data
    x_train, x_test, y_train, y_test, scaler = normalize_data("3.0")
    '''
    
    #Network Architecture
    model = Sequential()
    #layer 1 = LSTM w 50 neurons
    model.add(LSTM(100, return_sequences = True, input_shape = (x_train.shape[1], 1)))
    #layer 2 = LSTM w 50 neurons
    model.add(LSTM(100, return_sequences = False))
    #fully connected layer
    model.add(Dense(100, activation='relu'))
    #output layer (30 predictions)
    model.add(Dense(n_out))
    
    
    #Train Model
    model = train_model(model, x_train, x_test, y_train, y_test, "6.0")

if __name__ == "__main__":
    main_v4()