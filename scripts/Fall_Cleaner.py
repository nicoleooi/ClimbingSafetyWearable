# -*- coding: utf-8 -*-
"""
Created on Sun Feb 14 12:38:33 2021

@author: nicol
"""

import pandas as pd 
import os
import datetime
import scipy.interpolate as sc
import numpy as np
import math

path = "../data/"
dirs = ['D_indoor_watch', 'E_indoor_watch'] #get all directories

for d in dirs:
    files = os.listdir(path+d)
    
    for i in files:
        df = pd.read_csv(path+d+"/"+i)
        df = df[["accelerometerAccelerationX(G)", "accelerometerAccelerationY(G)", "accelerometerAccelerationZ(G)"]]
        df["accelerometerAccelerationX(G)"] = pd.to_numeric(df["accelerometerAccelerationX(G)"])
        df["accelerometerAccelerationY(G)"] = pd.to_numeric(df["accelerometerAccelerationY(G)"])
        df["accelerometerAccelerationZ(G)"] = pd.to_numeric(df["accelerometerAccelerationZ(G)"])

        name = i.replace(".csv", "")
        fullpath="../converted_data/"+name+"_onlyAcc.csv"
        df.to_csv(fullpath, index=False) 
        print(fullpath)