import pandas as pd 
import os

path = "../converted_data/"
dirs = os.listdir(path) #get all directories

for x in dirs:
    if x.find(".") != -1: #no files 
        pass
    f = os.listdir(path+x)
    for i in f:
        df = pd.read_csv(path+x+"/"+i)
        hr = df[["timestamp", "heart_rate"]].dropna()
        name = i.replace(".csv", "")
        fullpath=path+"hr_only/"+name+"_hr.csv"
        hr.to_csv(fullpath, index=False)
        #will not work fully for person C bc not same format yet 
        print(fullpath)

