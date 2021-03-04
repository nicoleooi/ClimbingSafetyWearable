# -*- coding: utf-8 -*-

import glob
import os
import pandas as pd
import numpy as np
import pickle
import hmmlearn 
from hmmlearn.hmm import GaussianHMM
from sklearn.preprocessing import MinMaxScaler

def make_supervised():
    #1 = fall
    #0 = not fall
    # for sisfall, columns are actually y, z, x
    path = "../../converted_data/sisFall_dataset/" 
    folders = os.listdir(path)
    
    for folder in folders:
        files = glob.glob(path+folder+"/*.csv")
        for f in files: #get all the csv files
            # if it starts w D, it's an ADL
            OG = pd.read_csv(path+f)
            
            df = pd.DataFrame()
            df["accelerometerAccelerationX(G)"] = OG["accelerometerAccelerationY(G)"]
            df["accelerometerAccelerationY(G)"] = OG["accelerometerAccelerationZ(G)"]
            df["accelerometerAccelerationZ(G)"] = OG["accelerometerAccelerationX(G)"]
            
            sp = f.split("_")
            trial = sp[-1]
            subject = sp[-2]
            
            if("F" in sp[-3]):
                df['Label'] = 1
                num = sp[-3].split("F")[-1]
                num = "F"+num
                
            else:
                df['Label'] = 0
                num = sp[-3].split("D")[-1]
                num = "D"+num
            
            df.to_csv('sup_fall_data/'+subject+"/"+num+"_"+subject+"_"+trial)
            print(num+"_"+subject+"_"+trial)
            

def extract_features(raw_data):
    features = pd.DataFrame()
    features['A_svm'] = np.sqrt(((raw_data["accelerometerAccelerationX(G)"]**2) + (raw_data["accelerometerAccelerationY(G)"]**2) +(raw_data["accelerometerAccelerationZ(G)"]**2)).astype(float))    
    features['theta'] = np.arctan((np.sqrt(((raw_data["accelerometerAccelerationX(G)"]**2)+(raw_data["accelerometerAccelerationZ(G)"]**2)).astype(float))/(raw_data["accelerometerAccelerationX(G)"])).astype(float))*(180/np.pi)
    features["A_dsvm"] = np.sqrt(((raw_data["accelerometerAccelerationX(G)"].diff())**2 + (raw_data["accelerometerAccelerationY(G)"].diff())**2 + (raw_data["accelerometerAccelerationZ(G)"].diff())**2).astype(float))
    features["A_gsvm"] = (features['theta']/90)*features["A_svm"]
    features["A_gdsvm"] = (features['theta']/90)*features["A_dsvm"]
    return features

'''
def rolling_window(features, name, ty):
    def to_supervised(df, n_input, col):
        x = []
        for i in range(df.shape[0]): #moving by 1, could move by n_input
            in_end = i + n_input
            if in_end <= df.shape[0]:
                x_input = np.array(df.iloc[i:in_end, col])
                x.append(x_input)
            
        x = np.array(x)
        
        return x
    #convert each feature into a rolling window
    n = 10
    a_svm = pd.DataFrame(to_supervised(features, n, 0))
    theta = pd.DataFrame(to_supervised(features, n, 1))
    a_dsvm = pd.DataFrame(to_supervised(features, n, 2))
    a_gsvm = pd.DataFrame(to_supervised(features, n, 3))
    a_gdsvm = pd.DataFrame(to_supervised(features, n, 4))
    
    pd.DataFrame(a_svm).to_csv("formatted_data/"+ty+"/"+name+"_asvm.csv")
    pd.DataFrame(theta).to_csv("formatted_data/"+ty+"/"+name+"_theta.csv")
    pd.DataFrame(a_dsvm).to_csv("formatted_data/"+ty+"/"+name+"_adsvm.csv")
    pd.DataFrame(a_gsvm).to_csv("formatted_data/"+ty+"/"+name+"_agsvm.csv")
    pd.DataFrame(a_gdsvm).to_csv("formatted_data/"+ty+"/"+name+"_agdsvm.csv")
'''

def format_data():
    path = "sup_fall_data/" 
    folders = os.listdir(path)
    
    #1786 = (24+70) simulated trials * 20 participants
    sets = [] #list of all observation sequences, each as an array
    lengths = []
    
    for folder in folders:
        files = glob.glob(path+folder+"/*.csv")
        for f in files:
            #get the name
            sp = f.split("_")
            trial = sp[-1]
            subject = sp[-2]
            if (("SA20" in subject) | ("SA21" in subject) | ("SA22" in subject) | ("SA23" in subject)):
                continue #don't collect training data for these people
            
            if("F" in sp[-3]):
                num = sp[-3].split("F")[-1]
                if ("07" in num):
                    continue
                num = "F"+num
                
            else:
                num = sp[-3].split("D")[-1]
                if not (("01" in num) | ("02" in num) | ("03" in num) | ("04" in num) | ("14" in num) | ("15" in num) | ("18" in num) | ("19" in num)):
                    continue
                num = "D"+num
                
            name = num+"_"+subject+"_"+trial
            
            data = pd.read_csv(f)
            data = data[["accelerometerAccelerationX(G)","accelerometerAccelerationY(G)","accelerometerAccelerationZ(G)"]]
            data["accelerometerAccelerationX(G)"] = pd.to_numeric(data["accelerometerAccelerationX(G)"], errors = 'coerce') 
            data["accelerometerAccelerationY(G)"] = pd.to_numeric(data["accelerometerAccelerationY(G)"], errors = 'coerce') 
            data["accelerometerAccelerationZ(G)"] = pd.to_numeric(data["accelerometerAccelerationZ(G)"], errors = 'coerce') 
            data = extract_features(data)
            data.dropna(axis=0, inplace=True)
            
            #add each sequence to the training set of sequences
            sequence = data.to_numpy() #array of rows, where each row is an array
            sets.append(sequence) #array of vectors at each element of X
            lengths.append(sequence.shape[0]) #length of sequence
            
            print(name)
            
    X = np.concatenate(sets)
    print(sum(lengths))
    print(X.shape[0])
    
    return X, np.array(lengths)

def fitHMM(X):
        #Normalize
        scaler = MinMaxScaler(feature_range=(0,1))
        X = scaler.fit_transform(X)
        
        shaped_X = np.reshape(X, [len(X), 5])
        model = GaussianHMM(n_components=2).fit(shaped_X)
        
        #fall = 1, no fall = 0
        hidden_states = model.predict(shaped_X)
        
        #get parameters of Gaussian HMM
        mus = np.array(model.means_)
        sigmas = np.array(np.sqrt(np.array([np.diag(model.covars_[0]),np.diag(model.covars_[1])])))
        P = np.array(model.transmat_)
        
        # find log-likelihood of Gaussian HMM
        logProb = model.score(np.reshape(X,[len(X),1]))
        
        # re-organize mus, sigmas and P so that first row is lower mean (if not already)
        if mus[0] > mus[1]:
            mus = np.flipud(mus)
            sigmas = np.flipud(sigmas)
            P = np.fliplr(np.flipud(P))
            hidden_states = 1 - hidden_states
            
        return hidden_states, mus, sigmas, P, logProb

def main():
    X, lengths = format_data()
    #X has shape (num samples, num features)
    
    # load data we want to classify (training data?)
    df = X[0:(lengths[0]-1)] #first set of training data
     
    #Normalize
    scaler = MinMaxScaler(feature_range=(0,1))
    df = scaler.fit_transform(df)
    
    # log transform the data and fit the HMM
    log_data = np.log(df)
    hidden_states, mus, sigmas, P, logProb = fitHMM(log_data)
    print("hi")

if __name__ == "__main__":
    main()

            
            
            