# -*- coding: utf-8 -*-
"""
Created on Mon Nov 30 20:53:08 2020

@author: nicol
"""
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.animation as animation
import time
from matplotlib import style

def graph(f, e):
    df = pd.read_csv(f)
    
    ys = df.iloc[:,0].values #actual val
    ys1 = df.iloc[:,1].values #prediction
    fig, axs = plt.subplots(3)
    fig.suptitle('One second ahead HR prediction')
            
    axs[0].plot(ys)
    axs[1].plot(ys1)
    
    axs[0].title.set_text("Actual HR")
    axs[1].title.set_text("Predicted HR")
    
    axs[0].set_ylim([0, 200])
    axs[1].set_ylim([0, 200])
    
    if e == "abs":
        err = abs(ys1-ys)
        axs[2].plot(err)
        axs[2].title.set_text("Absolute Error")
        axs[2].set_ylim([0, 200])
        
    elif e == "mse":
        err = ((ys-ys1)**2)/len(ys)
        axs[2].plot(err)
        axs[2].title.set_text("Mean Squared Error")
        axs[2].set_ylim([0, 5])
        
    elif e == "rms":
        err = (((ys-ys1)**2)/len(ys))**0.5
        axs[2].plot(err)
        axs[2].title.set_text("Root Mean Squared Error")
        axs[2].set_ylim([0, 5])
    
    plt.show(block=True)