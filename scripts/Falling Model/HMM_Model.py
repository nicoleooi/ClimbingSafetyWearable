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
    path = "../../data/sisFall_acc_only/*/" 
    data = []

    for f in glob.glob(path+"D*"):
        # if it startsw D, it's an ADL
        if("D" in f):
            
            