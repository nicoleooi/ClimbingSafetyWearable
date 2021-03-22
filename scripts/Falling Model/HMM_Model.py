# -*- coding: utf-8 -*-

import glob
import os
import pandas as pd
import numpy as np
import joblib
import hmmlearn 
from hmmlearn.hmm import GaussianHMM
from sklearn.preprocessing import MinMaxScaler
from matplotlib import pyplot as plt
import seaborn as sns
from scipy import stats as ss

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
    adl_sets = [] #list of all observation sequences, each as an array
    adl_lengths = []
    anames = []
    astarts = []
    aends = []
    anext_start = 0
    
    fall_sets = []
    fall_lengths = []
    fnames = []
    fstarts = []
    fends = []
    fnext_start = 0

    
    fall = 0; #fall is false

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
                if (("07" in num) | ("08" in num) | ("09" in num) | ("10" in num) | ("11" in num) | ("12" in num) | ("13" in num) | ("14" in num) | ("15" in num)):
                    continue
                num = "F"+num
                fall = 1
                
            else:
                num = sp[-3].split("D")[-1]
                if not (("01" in num) | ("02" in num) | ("03" in num) | ("04" in num) | ("15" in num) | ("18" in num) | ("19" in num)):
                    continue
                num = "D"+num
                fall = 0
                
            name = num+"_"+subject+"_"+trial
            
            data = pd.read_csv(f)
            data = data[["accelerometerAccelerationX(G)","accelerometerAccelerationY(G)","accelerometerAccelerationZ(G)"]]
            data["accelerometerAccelerationX(G)"] = pd.to_numeric(data["accelerometerAccelerationX(G)"], errors = 'coerce') 
            data["accelerometerAccelerationY(G)"] = pd.to_numeric(data["accelerometerAccelerationY(G)"], errors = 'coerce') 
            data["accelerometerAccelerationZ(G)"] = pd.to_numeric(data["accelerometerAccelerationZ(G)"], errors = 'coerce') 
            data = extract_features(data)
            data.dropna(axis=0, inplace=True)
            
            #add each sequence to the correct training set of sequences
            sequence = data.to_numpy() #array of rows, where each row is an array
            
            if(fall): #fall data organized separately
                fall_sets.append(sequence) #array of vectors at each element of X
                fall_lengths.append(sequence.shape[0]) #length of sequence
                #track the start and end of each training file so I know where it sits in the list
                fstarts.append(fnext_start)
                fends.append(fnext_start + sequence.shape[0] -1)
                fnext_start = fnext_start + sequence.shape[0]
                fnames.append(name) #track the name so I can print the index
            
            else: #do the same for adl separately
                adl_sets.append(sequence) #array of vectors at each element of X
                adl_lengths.append(sequence.shape[0]) #length of sequence
                #track the start and end of each training file so I know where it sits in the list
                astarts.append(anext_start)
                aends.append(anext_start + sequence.shape[0] -1)
                anext_start = anext_start + sequence.shape[0]
                anames.append(name) #track the name so I can print the index            
            
            print(name)
            
    fall_X = np.concatenate(fall_sets)
    adl_X = np.concatenate(adl_sets)
    
    writer = pd.ExcelWriter('fall_training_data.xlsx', engine = 'xlsxwriter')
    pd.DataFrame(fnames).to_excel(writer, sheet_name= "names", index=False, header=False)
    pd.DataFrame(fstarts).to_excel(writer, sheet_name= "starts", index=False, header=False)
    pd.DataFrame(fends).to_excel(writer, sheet_name= "ends", index=False, header=False)
    writer.save()
    
    writer = pd.ExcelWriter('adl_training_data.xlsx', engine = 'xlsxwriter')
    pd.DataFrame(anames).to_excel(writer, sheet_name= "names", index=False, header=False)
    pd.DataFrame(astarts).to_excel(writer, sheet_name= "starts", index=False, header=False)
    pd.DataFrame(aends).to_excel(writer, sheet_name= "ends", index=False, header=False)
    writer.save()
    
    return fall_X, adl_X, np.array(fall_lengths), np.array(adl_lengths)

def fitHMM(X, lengths, n_states, iters, file_path):
        '''
        #Normalize
        scaler = MinMaxScaler(feature_range=(0,1))
        X = scaler.fit_transform(X)
        '''
        shaped_X = np.reshape(X, [len(X), 5])
        
        #n_components = 2 bc 2 hidden states
        #pass in shaped data
        #pass in lengths
        
        model = GaussianHMM(n_components=n_states, n_iter=iters, tol = 0.5, verbose=True)
        #add convergence monitor here
        model.fit(shaped_X, lengths)
        print(model.monitor_.converged)
        
        #fall = 1, no fall = 0
        hidden_states = model.predict(shaped_X)
        
        #get final parameters of Gaussian HMM
        mus = np.array(model.means_)
        sigmas = np.array(np.sqrt(np.array([np.diag(model.covars_[0]),np.diag(model.covars_[1])])))
        P = np.array(model.transmat_)
        
        #pickle the model
        joblib.dump(model, file_path)

        return hidden_states, mus, sigmas, P, model


def train(iters):
    #chose 6 types of falls and 7 adls to train on 
    fall_states = 6
    adl_states = 7
    fall_X, adl_X, fall_lengths, adl_lengths = format_data()
    #X has shape (num samples, num features)
    
    # load data we want to classify (training data?)
    f_hidden_states, f_mus, f_sigmas, f_P, fall_model = fitHMM(fall_X, fall_lengths, fall_states, iters, "models/fall_models/model_v2_"+str(iters)+".joblib")
    a_hidden_states, a_mus, a_sigmas, a_P, adl_model = fitHMM(adl_X, adl_lengths, adl_states, iters, "models/adl_models/model_v2_"+str(iters)+".joblib")

    return fall_model, adl_model
    
def test(fall_model, adl_model, iters):
    path = "sup_fall_data/" 
    folders = os.listdir(path)
    results = []
    
    for folder in folders:
        files = glob.glob(path+folder+"/*.csv")
        for f in files:
            #get the name
            sp = f.split("_")
            trial = sp[-1]
            subject = sp[-2]
            if not (("SA20" in subject) | ("SA21" in subject) | ("SA22" in subject) | ("SA23" in subject)):
                continue #these people were in the training category, not testing category
            
            if("F" in sp[-3]):
                num = sp[-3].split("F")[-1]
                if (("07" in num) | ("08" in num) | ("09" in num) | ("10" in num) | ("11" in num) | ("12" in num) | ("13" in num) | ("14" in num) | ("15" in num)):
                    continue
                num = "F"+num
                fall = 1
                
            else:
                num = sp[-3].split("D")[-1]
                if not (("01" in num) | ("02" in num) | ("03" in num) | ("04" in num) | ("15" in num) | ("18" in num) | ("19" in num)):
                    continue
                num = "D"+num
                fall = 0
                
            name = num+"_"+subject+"_"+trial
            rslt = tuple()
            
            data = pd.read_csv(f)
            data = data[["accelerometerAccelerationX(G)","accelerometerAccelerationY(G)","accelerometerAccelerationZ(G)"]]
            data["accelerometerAccelerationX(G)"] = pd.to_numeric(data["accelerometerAccelerationX(G)"], errors = 'coerce') 
            data["accelerometerAccelerationY(G)"] = pd.to_numeric(data["accelerometerAccelerationY(G)"], errors = 'coerce') 
            data["accelerometerAccelerationZ(G)"] = pd.to_numeric(data["accelerometerAccelerationZ(G)"], errors = 'coerce') 
            data = extract_features(data)
            data.dropna(axis=0, inplace=True)
            
            #if length of data longer than 15s, split into 15s or less
            if(len(data.index) > 15):
                for i in range(0, len(data.index), 15):
                    if(i+15 < len(data.index)): #if it's not past the end of the test sequence                     
                        smol = data.iloc[i:i+15, :] #iloc is EXCLUSIVE of the upper bound
                    else:
                        smol = data.iloc[i:len(data.index), :]
                        
                    sequence = smol.to_numpy() #array of rows, where each row is an array
                    fall_score = fall_model.score(sequence)
                    adl_score = adl_model.score(sequence)
            #add each sequence to the correct training set of sequence
            else:
                sequence = data.to_numpy() #array of rows, where each row is an array
                fall_score = fall_model.score(sequence)
                adl_score = adl_model.score(sequence)
            
            prediction = 0
            if( fall_score > adl_score):
                prediction = 1
            
            rslt = (name, fall_score, adl_score, prediction, fall)
            results.append(rslt)
            print(name)
    df = pd.DataFrame(results, columns = ["sample", "fall_score", "adl_score", "class", "truth_label" ])
    df.to_csv("models/tested_"+str(iters)+".csv", index=False)
          

if __name__ == "__main__":
    fall_model, adl_model = train(100)
    #fall_model = joblib.load("models/fall_models/model_100.joblib")
    #adl_model = joblib.load("models/adl_models/model_100.joblib")
    test(fall_model, adl_model, 100)

            
            
            