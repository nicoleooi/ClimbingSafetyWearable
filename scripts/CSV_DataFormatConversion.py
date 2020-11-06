#This script is to be used on csv files that aren't in a format consistent with
#the other data we have.

import csv
import os
import pandas as pd
import pytz

def main():

    #Reading Data:
    filename = 'Georgia_Lewin-LaFrance_2020-09-03_18-21-58.csv'
    data = pd.read_csv(filename, usecols=[1,2])

    #Getting Timestamps:
    data.rename(columns={'Sport':'Time'}, inplace=True)
    times = data.Time
    del times[0]
    del times[1]
    #print(times)

    #Getting Heart Rate:
    data.rename(columns={'Date':'HR'}, inplace=True)
    hr = data.HR
    del hr[0]
    del hr[1]
    #print(hr)

    #Drop the two irelevant rows
    data.drop(data.index[[0,1]]);
                
    #Writing Data:
    data.to_csv('test2.csv', index = False);

if __name__=='__main__':
    main()
