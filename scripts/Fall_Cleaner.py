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
import csv
import itertools
import glob

def clean_sisFall():
    path = "../data/sisFall_acc_only/"
    folders = os.listdir(path)
    for s in folders:
        fullpath = path+s
        files = os.listdir(fullpath)
        for f in files:
            if("SA" not in f):
                continue
            print(f)
            og = pd.read_csv(fullpath+"/"+f, parse_dates=True,header = 0)

            df = og[['ADXL345 X', 'ADXL345 Y', 'ADXL345 Z']] #get first 3 columns
            df.columns = ['accelerometerAccelerationX(G)', 'accelerometerAccelerationY(G)', 'accelerometerAccelerationZ(G)']
            df = df*((2*16)/(2**13))
                
            df.to_csv("../converted_data/sisFall_dataset/"+s+"/"+f)
            

def convert_sisFall():
    path = "../data/"
    dirs = ['SisFall_dataset'] #get all directories
    flag = 0
    for d in dirs: #get into sisfall folder
        folders = os.listdir(path+d)
        for s in folders: #get into each subject's folder
            fullpath = path+d+"/"+s
            files = os.listdir(fullpath)
            for i in files: #get each file
                if("txt" not in i):
                    continue
                if("D17_SE08_R05" in i):
                    flag = 1
                if (flag == 0):
                    continue
                lines = []
                with open(fullpath+"/"+i, 'r') as in_file:
                    print(i)
                    for line in in_file:
                        stripped = line.strip()
                        stripped = stripped.replace(" ", "")
                        stripped = stripped.replace(";", "")
                        lines.extend(stripped.split(","))
                arr = np.array(lines)
                arr = arr.reshape(int(len(arr)/9), 9)
                df = pd.DataFrame(arr, columns = ['ADXL345 X', 'ADXL345 Y', 'ADXL345 Z', 'rotation X', 'rotation Y', 'rotation Z', 'MMA8451Q X', 'MMA8451Q Y','MMA8451Q Z' ])
                
                name = i.replace(".txt", "")
                filepath=path+"sisFall_acc_only/"+s+"/"+name+".csv"
                df.to_csv(filepath, index=False)
            
def clean_collected():
    #this code is for the data alex and i collected ourselves
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
            
if __name__ == "__main__":
    clean_sisFall()
