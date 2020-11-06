import pandas as pd 
import os

path = "../converted_data/"
dirs = os.listdir(path) #get all directories

for x in dirs:
    if x.find(".") != -1: #no files 
        pass
    print(path+x)
    f = os.listdir(path+x)
    for i in f:
        df = pd.read_csv(path+x+f)
    # delete blank rows
    # write to CSV

