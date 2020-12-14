# -*- coding: utf-8 -*-
"""
Created on Wed Dec  9 15:18:53 2020

@author: Alexander Ingham
"""
import os
import glob
import pandas as pd
import numpy as np
import keras
from hmmlearn.hmm import MultinomialHMM
import warnings

"""
conda install -c omnia hmmlearn
Used to install necessary hmm library in conda
"""

path = "../../data\E_indoor_watch/2020*.csv"
appended_data = []

for f in glob.glob(path):
    df = pd.read_csv(f, parse_dates=True,header = 0)
    appended_data.append(df)
    
df = pd.concat(appended_data)
#df.to_csv('appended.csv')          To visualize appended data in csv file


#HMM:
startprob = np.array([0.5, 0.5])    #Will need to change these

transmat =  np.array([[0.8, 0,2],
                    [0.2, 0.8]])

covar =  np.array([[0.9, 0,1],
                    [0.2, 0.8]])

h = MultinomialHMM(n_components = 2, startprob = startprob, transmat = transmat)

#h.fit(data)
print(h.transmat_)
#prob = h.decode()
print(np.exp(prob[0]))
x, z = h.sample(500)
print(x)