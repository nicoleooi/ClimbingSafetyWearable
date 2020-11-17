#This script is to be used on csv files that aren't in a format consistent with
#the other data we have.

import csv
import os
import pandas as pd
import pytz

def clean(filename, path):

    #Reading Data:
    data = pd.read_csv(path+"/"+filename, usecols=[1,2])

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
    data = data.drop([0]).drop([1])
                
    #Writing Data:
    fullpath = path.replace("C_outdoor_polarstrap", "")+"/hr_only/"+filename.replace(".csv", "")+"_hr.csv"
    data.to_csv(fullpath, index = False);

if __name__=='__main__':
    path = "../converted_data/C_outdoor_polarstrap"
    f = os.listdir(path)
    
    for file in f:
        clean(file, path)
        print(file)

