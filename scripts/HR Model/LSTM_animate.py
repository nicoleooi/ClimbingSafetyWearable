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


'''
def animate():
    df = pd.read_csv('real_time_HR.csv')
    ys = df.iloc[:,0].values #actual val
    ys1 = df.iloc[:,1].values #prediction
    
    
    if len(ys) >= 120: #only plot 2 min at a time
        ys = df.iloc[-120: 0].values
        ys1 = df.iloc[-120: 1].values
    
        
    xs = list(range(1, len(ys)+1))
    ax1.clear()
    ax1.plot(xs, ys)
    ax1.plot(xs, ys1)
    
    ax1.set_title('One second ahead HR forecasting (bpm)', fontsize=32)
    ax1.legend(['Actual','Prediction'], loc='lower right')
'''
 
'''
style.use('fivethirtyeight')
fig = plt.figure()
fig.figsize = (8,4)
ax1 = fig.add_subplot(1,1,1)

#ani = animation.FuncAnimation(fig, animate)

plt.tight_layout()

plt.show()
'''
df = pd.read_csv('real_time_HR.csv')
ys = df.iloc[:,0].values #actual val
ys1 = df.iloc[:,1].values #prediction
diff = abs(ys1-ys)
fig, axs = plt.subplots(3)
fig.suptitle('One second ahead HR prediction')
axs[0].plot(ys)
axs[1].plot(ys1)
axs[2].plot(diff)

axs[0].set_ylim([0, 200])
axs[1].set_ylim([0, 200])
axs[2].set_ylim([0, 200])

plt.show(block=True)