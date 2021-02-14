import pandas as pd 
import os
import datetime
import scipy.interpolate as sc
import numpy as np
import math

path = "../converted_data/"
dirs = ['A_outdoor_watch', 'C_outdoor_polarStrap'] #get all directories

for d in dirs:
    if d.find(".") != -1: #no files
        pass
        
    files = os.listdir(path+d)
    for i in files:
        df = pd.read_csv(path+d+"/"+i)
        
        if("Georgia" in i):
        
            hr = df.drop(df.index[[0,1]])
            hr.drop(['Name'], axis = 1, inplace = True)
            hr = hr[['Sport', 'Date']]
            hr.columns = ['Time', 'HR']
            hr['HR'] = pd.to_numeric(hr['HR'])
            hr['Time'] = pd.to_datetime(hr['Time'])
            hr['Time'] = hr['Time']-hr.iloc[0, 0]  
            hr['Time'] = hr['Time'].dt.total_seconds()
            hr.drop_duplicates('Time', inplace=True, keep='last')
            hr = hr.query('HR > 0')
            
            hr.dropna(subset=['HR'], inplace=True)
            
            x = np.array(hr['Time'])
            y = np.array(hr['HR'])
            f = sc.InterpolatedUnivariateSpline(x,y, k=3)
            xnew = np.array(range(0, int(hr.iloc[-1,0])+1))
            ynew = f(xnew)
            
            df2 = pd.DataFrame()
            df2['Time'] = xnew
            df2['HR'] = ynew
            
            name = i.replace(".csv", "")
            fullpath=path+"data_in2/"+name+"_hr.csv"
            df2.to_csv(fullpath, index=False) 
            print(fullpath)
            
            pass
        
        elif not ("Elhana" in i):
            hr = df[["timestamp", "heart_rate"]]
            
            hr.columns = ['Time', 'HR']
            hr['HR'] = pd.to_numeric(hr['HR'])
            hr['Time'] = pd.to_datetime(hr['Time'])
            hr['Time'] = hr['Time']-hr.iloc[0, 0]  
            hr['Time'] = hr['Time'].dt.total_seconds()
            hr.drop_duplicates('Time', inplace=True, keep='last')
            hr = hr.query('HR > 0')
            hr.dropna(subset=['HR'], inplace=True)
            
            x = np.array(hr['Time'])
            y = np.array(hr['HR'])
            f = sc.InterpolatedUnivariateSpline(x,y, k=3)
            xnew = np.array(range(0, int(hr.iloc[-1,0])+1))
            ynew = f(xnew)
            
            df2 = pd.DataFrame()
            df2['Time'] = xnew
            df2['HR'] = ynew
            
            name = i.replace(".csv", "")
            fullpath=path+"data_in2/"+name+"_hr.csv"
            df2.to_csv(fullpath, index=False)
            #will not work fully for person C bc not same format yet 
            print(fullpath)
        '''    
        else:
            hr = df[["timestamp", "heart_rate"]]
            
            hr.columns = ['Time', 'HR']
            hr['Time'] = pd.to_datetime(hr['Time'])
            hr['Time'] = hr['Time']-hr.iloc[0, 0]  
            hr['Time'] = hr['Time'].dt.total_seconds()
            hr.drop_duplicates('Time', inplace=True, keep='last')
            
            gap = []
            for j in range(0,len(hr)-1):
                if (hr.iloc[j+1, 0] - hr.iloc[j, 0]) > 1:
                    start = int(hr.iloc[j, 0])
                    end = int(hr.iloc[j+1, 0])
                    gap.extend(range(start+1, end))
            
            df = pd.DataFrame(gap, columns=['Time'])
            df['HR'] = -1
            hr = hr.append(df)
            hr.fillna(-1, inplace=True)
            hr.sort_values('Time', inplace=True)
            
            name = i.replace(".csv", "")
            fullpath=path+"new_interpolated_hr_only/"+name+"_hr.csv"
            hr.to_csv(fullpath, index=False)
            #will not work fully for person C bc not same format yet 
            print(fullpath)
        
        '''
            
        
