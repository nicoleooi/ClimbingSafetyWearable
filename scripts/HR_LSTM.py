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

path = "../converted_data/hr_only/Georgia*.csv"
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

from sklearn.preprocessing import MinMaxScaler
scaler = MinMaxScaler(feature_range=(0,1))