import pandas as pd 
import os
import datetime

path = "../converted_data/"
dirs = os.listdir(path) #get all directories

for x in dirs:
    if x.find(".") != -1: #no files 
        pass
    f = os.listdir(path+x)
    for i in f:
        df = pd.read_csv(path+x+"/"+i)
        hr = df[["timestamp", "heart_rate"]].dropna()
        hr.columns = ['Time', 'HR']
        
        #if time includes date, take it out
        hr['Time'] = pd.to_datetime(hr['Time'])
        for t, row in hr.iterrows():
            hr.loc[t, "Time"] = hr.loc[t,"Time"].strftime("%X")
        
        name = i.replace(".csv", "")
        fullpath=path+"hr_only/"+name+"_hr.csv"
        hr.to_csv(fullpath, index=False)
        #will not work fully for person C bc not same format yet 
        print(fullpath)

