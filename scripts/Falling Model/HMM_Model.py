# -*- coding: utf-8 -*-
"""
Created on Mon Feb 15 20:23:27 2021

@author: nicol
"""
import glob
import pandas as pd

def make_supervised():
    #1 = fall
    #0 = not fall
    path = "../../converted_data/sisFall_dataset/" 

    for f in glob.glob(path+"*/*.csv"): #get all the csv files
        # if it startsw D, it's an ADL
        df = pd.read_csv(path+f)
        if("D" in f):
            df['Label'] = 0
        else:
            df['Label'] = 1
        df.to_csv('sup_fall_data/'+f)
            
            
            