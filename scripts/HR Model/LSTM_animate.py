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

plt.show()
plt.savefig('tested.jpg')