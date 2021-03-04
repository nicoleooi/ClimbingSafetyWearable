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
from random import shuffle

from keras.models import Sequential
from keras.layers import Dense, Activation, Masking, LSTM
from keras.callbacks import CSVLogger
#dense is for output layer
#CuDNNLSTM is if we can use a GPU
from keras import optimizers
from sklearn.preprocessing import MinMaxScaler
from LSTM_animate import graph

def format_data(version, n_in, n_out):
    '''
    Formats data and returns the formatted arrays.
    '''
    print("Formatting data...")
    
    path = "../../converted_data/data_in2/*.csv" 
    data = []

    for f in glob.glob(path):
        df = pd.read_csv(f, parse_dates=True,header = 0)
        data.append(df)

    #change order from 0-26 to 
    a_idx = 0
    b_idx = 3
    c_idx = 16

    
    train_data=[]
    train_idx = []
    
    for i in range(0,12):
        if (i == 0) | (i == 11): #add A
            train_data.append(data[a_idx])
            train_idx.append(a_idx)
            a_idx = a_idx+1
            '''
        elif (i % 2) == 0: #add B
            train_data.append(data[b_idx])
            train_idx.append(b_idx)
            b_idx=b_idx+1
        else: #add C
            train_data.append(data[c_idx])
            train_idx.append(c_idx)
            c_idx=c_idx+1
            '''
        else:
            train_data.append(data[b_idx])
            train_idx.append(b_idx)
            b_idx=b_idx+1
    
    df = pd.concat(train_data) #dataframe of all training climbs
    n_train = len(df)
    
    for i in range(0, len(data)):
        if i in train_idx:
            pass
        else:
            print(i)
            df = df.append(data[i])
    
    df.to_csv('appended.csv')

    df['HR'] = pd.to_numeric(df['HR'], errors = 'coerce') 
    #df.drop(['Date Time'], axis = 1, inplace = True)
    df.drop(['Time'], axis = 1, inplace = True)
    
    def to_supervised(df, n_input, n_output):
        x = []
        y = []
        for i in range(df.shape[0]): #moving by 1, could move by n_input
            in_end = i + n_input
            out_end = in_end + n_output
            if out_end <= df.shape[0]:
                x_input = np.array(df.iloc[i:in_end, 0])
                y_input= np.array(df.iloc[in_end:out_end,0])
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

    #train on 80% of data, 21 climbs
    x_train, x_test = x[0:n_train-1], x[n_train:-1]
    y_train, y_test = y[0:n_train-1], y[n_train:-1]

    # num samples, num time stamps, num features
    x_train = np.reshape(x_train, (x_train.shape[0], x_train.shape[1], 1))
    x_test = np.reshape(x_test, (x_test.shape[0], x_test.shape[1], 1))
    
    print("Finished formatting data")

    return x_train, x_test, y_train, y_test, n_train, scaler


def normalize_data(version, n_train):
    '''Data Normalization'''
    print("Starting data normalization...")
    
    x = pd.read_csv('./formatted_data/dropped_HR_x_v'+version+'.csv', header = 0)
    x.drop(x.columns[0],inplace=True, axis=1)
    y = pd.read_csv('./formatted_data/dropped_HR_y_v'+version+'.csv', header = 0)
    y.drop(y.columns[0], inplace=True, axis=1)

    '''Data Normalization'''

    scaler = MinMaxScaler(feature_range=(0,1))

    x = scaler.fit_transform(x)
    y = scaler.fit_transform(y)

    #train on 80% of data, 21 climbs
    x_train, x_test = x[0:n_train-1], x[n_train:-1]
    y_train, y_test = y[0:n_train-1], y[n_train:-1]

    # num samples, num time stamps, num features
    x_train = np.reshape(x_train, (x_train.shape[0], x_train.shape[1], 1))
    x_test = np.reshape(x_test, (x_test.shape[0], x_test.shape[1], 1))
    
    print("Finished normalizing data")

    return x_train, x_test, y_train, y_test, scaler
    
def train_model(model, x_train, x_test, y_train, y_test, version):
    '''Model checks and tools to stop overfitting'''
    print("Training model")
    from keras.callbacks import ModelCheckpoint, EarlyStopping
    filepath = 'models_v'+version+'/{epoch:02d}-{loss:.4f}-{val_loss:.4f}-{mae:.4f}.hdf5'
    
    savedModel = ModelCheckpoint(filepath, monitor='val_loss', save_best_only=True, mode='min')
    earlyStopper = EarlyStopping(monitor = 'val_loss', patience = 50)
    #tbCallBack = keras.callbacks.TensorBoard(log_dir='./models_v'+version+'/tblogs' , histogram_freq=0, write_graph=True, write_images=False)
    csv_logger = CSVLogger('./models_v'+version+'/models_v'+version+'history.csv', append=True)
    cbs = [savedModel, earlyStopper, csv_logger]
    
    nu = 0.001 #learning rate was 0.0001 before
    optimizers.Adam(lr=nu)
        
    model.compile(optimizer='adam', loss='mse', metrics=['mse','mae'])
    model.fit(x_train, y_train, validation_split=0.3, epochs=100, callbacks=cbs, batch_size=32)
    #num epochs = number of passes of training
    #batch size = number of samples to work through before updating internal params

    
    return model

def test_model(model, scaler, x_train, x_test, y_train, y_test, version, n_out):
    print("Starting testing...")
    
    #the best model will be the LAST file in the /models folder
    path="./models_v"+version+"/"
    files = os.listdir(path)
    best_model = files[-1]
    print(best_model)
    model.load_weights("./models_v"+version+"/"+best_model)
    joblib.dump(model, './best_models/hrpredictor_v'+version+'.joblib')

    actual_csv = []
    pred_csv = []
    writer = pd.ExcelWriter('real_time_HR_v'+version+".xlsx")
    
    for i in range(0, x_test.shape[0]):
        x_input = x_test[i, :, :]
        x_input = np.reshape(x_input, (1, x_input.shape[0], 1)) #1 sample, time stamp is 0, then 1 feature
        prediction = model.predict(x_input)
        prediction = scaler.inverse_transform(prediction)
        
        y_input = y_test[i,:]
        y_input = np.reshape(y_input, (1,n_out))
        actual = scaler.inverse_transform(y_input)
        
        actual_csv.append(actual)
        pred_csv.append(prediction)
        
    
    actual_csv = np.reshape(np.array(actual_csv), (len(actual_csv), n_out))
    pred_csv = np.reshape(np.array(pred_csv), (len(pred_csv), n_out))
    
    pd.DataFrame(actual_csv).to_excel(writer, 'actual', index=False, header=False)
    pd.DataFrame(pred_csv).to_excel(writer, 'prediction', index=False, header=False)
    writer.save()
    
    print("Finished testing")
    return

def main():
    '''
    Date: Feb 1, 2021
    2 LSTM layers w 50 neurons each
    1 Dense layer w 50 neurons
    1 Dense output layer
    batch size = 32
    100 epochs, early stop at 50
    nu = 0.0001
    30%
    model #18
    im on the verge of a breakdown i stg dont touch any of these params oh my god
    '''
    n_in = 15
    n_out = 10
    n_train = 0
    
    #Format Data
    x_train, x_test, y_train, y_test, n_train, scaler = format_data("18.0", n_in, n_out)
    
    #Normalize Data
    #x_train, x_test, y_train, y_test, scaler = normalize_data("18.0", n_train)
    
    #Network Architecture
    model = Sequential()
    #mask missing values
    #model.add(Masking(mask_value=-1, input_shape = (x_train.shape[1], 1)))
    #layer 1 = LSTM w 50 neurons
    model.add(LSTM(20, return_sequences = True, input_shape = (x_train.shape[1], 1)))
    #layer 2 = LSTM w 50 neurons
    model.add(LSTM(20, return_sequences = False))
    #fully connected layer
    model.add(Dense(20, activation='relu'))
    #output layer (30 predictions)
    model.add(Dense(n_out))
    
    
    #Train Model
    #model = train_model(model, x_train, x_test, y_train, y_test, "18.0")
    
    
    #Test Model
    test_model(model, scaler, x_train, x_test, y_train, y_test, "18.0", n_out)
  

def make_graph():
    graph("real_time_HR_v3.0.csv", "mse")
    
if __name__ == "__main__":
    main()