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
from keras.models import Sequential
from keras.layers import Dense, Activation, CuDNNLSTM, LSTM
#dense is for output layer
#CuDNNLSTM is if we can use a GPU
from keras import optimizers

def format_data():
    '''
    Formats data and returns the formatted arrays.
    '''
    print("Formatting data...")
    
    path = "../../converted_data/hr_only/Georgia*.csv"
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

    #how many time steps should we be using to predict the next step?
    #for now, use 49 to predict the next one
    x = [] #features
    y = [] #labels 
    for i in range(0, df.shape[0]-48):
        x.append(df.iloc[i:i+48, 3])
        y.append(df.iloc[i+48, 3])

    x, y = np.array(x), np.array(y)
    y = np.reshape(y, (len(y), 1))

    #delete every other row in a rolling window
    #features = x
    #labels = y (truth labels for evaluating)
    x = np.delete(x, list(range(1, x.shape[1], 2)), axis=1)
    x = np.delete(x, list(range(1, x.shape[0], 2)), axis=0) 
    y = np.delete(y, list(range(1, y.shape[0], 2)), axis=0)

    pd.DataFrame(x).to_csv('dropped_HR_x.csv')
    pd.DataFrame(y).to_csv('dropped_HR_y.csv')

    '''Data Normalization'''

    from sklearn.preprocessing import MinMaxScaler
    scaler = MinMaxScaler(feature_range=(0,1))

    x = scaler.fit_transform(x)
    y = scaler.fit_transform(y)

    #train on one climb? 
    split = 5156
    x_train, x_test = x[:-split], x[-split:]
    y_train, y_test = y[:-split], y[-split:]

    # num samples, num time stamps, num features
    x_train = np.reshape(x_train, (x_train.shape[0], x_train.shape[1], 1))
    x_test = np.reshape(x_test, (x_test.shape[0], x_test.shape[1], 1))
    
    print("Finished formatting data")

    return x_train, x_test, y_train, y_test

def normalize_data():
    '''Data Normalization'''
    print("Starting data normalization...")
    
    x = pd.read_csv('dropped_HR_x.csv', header = 0)
    x.drop(x.columns[0],inplace=True, axis=1)
    y = pd.read_csv('dropped_HR_y.csv', header = 0)
    y.drop(y.columns[0], inplace=True, axis=1)

    from sklearn.preprocessing import MinMaxScaler
    scaler = MinMaxScaler(feature_range=(0,1))

    x = scaler.fit_transform(x)
    y = scaler.fit_transform(y)

    #train on one climb 
    split = 5156
    x_train, x_test = x[:-split], x[-split:]
    y_train, y_test = y[:-split], y[-split:]

    # num samples, num time stamps, num features
    x_train = np.reshape(x_train, (x_train.shape[0], x_train.shape[1], 1))
    x_test = np.reshape(x_test, (x_test.shape[0], x_test.shape[1], 1))
    
    print("Finished data normalization")
    
    return x_train, x_test, y_train, y_test
    
def train_model(model, x_train, x_test, y_train, y_test):
    '''Model checks and tools to stop overfitting'''
    print("Formatting model")
    from keras.callbacks import ModelCheckpoint, EarlyStopping
    filepath = 'models/{epoch:02d}-{loss:.4f}-{val_loss:.4f}-{val_mae:.4f}-{val_mae:.4f}.hdf5'
    callback = [EarlyStopping(monitor = 'val_loss', patience = 50),
                ModelCheckpoint(filepath, monitor='loss', save_best_only=True, mode='min')]

    nu = 0.0001 #learning rate
    optimizers.Adam(lr=nu)
    model.compile(optimizer='adam', loss='mse', metrics=['mae'])
    model.fit(x_train, y_train, validation_split=0.2, epochs=100, callbacks=callback, batch_size=8)
    #num epochs = number of passes of training
    #batch size = number of samples to work through before updating internal params
    return model

def test_model(model, x_train, x_test, y_train, y_test):
    print("Starting testing...")
    
    #the best model will be the LAST file in the /models folder
    files = os.listdir("./models")
    best_model = files[-1]
    print(best_model)
    model.load_weights("./models/"+best_model)
    
    import time
    #USE A WHILE LOOP FOR REAL TIME PREDICTION
    #TO UPDATE THE CSV FILES
    from sklearn.preprocessing import MinMaxScaler
    scaler = MinMaxScaler(feature_range=(0,1))
    obj_x = scaler.fit(x)
    obj_y = scaler.fit(y)

    for i in range(0, x_test.shape[0]):
        hr_summary = [] #forecasted hr
        x_input = x_test[i, :, :]
        x_input = np.reshape(x_input, (1, x_input.shape[0], 1)) #1 sample, time stamp is 0, then 1 feature
        x_input = model.predict(x_input)
        prediction = scaler.inverse_transform(x_input)
        
        y_input = y_test[i,:]
        y_input = np.reshape(y_input, (1,1))
        actual = scaler.inverse_transform(y_input)
        
        hr_summary.append(actual)
        hr_summary.extend(prediction)
        
        df = pd.DataFrame(hr_summary)
        df = df.T #put it into a single row
        #df.to_csv("real_time_HR.csv", mode='a', header=False, index=False)
        
    
    print("Finished testing")
    return

def main():
    x_train, x_test, y_train, y_test = normalize_data()
    
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
    
    #model = train_model(model, x_train, x_test, y_train, y_test)
    
    test_model(model, x_train, x_test, y_train, y_test)
    #model.load_weights()
    
if __name__ == "__main__":
    main()