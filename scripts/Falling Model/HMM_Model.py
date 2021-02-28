# -*- coding: utf-8 -*-

import glob
import os
import pandas as pd

def make_supervised():
    #1 = fall
    #0 = not fall
    # for sisfall, columns are actually y, z, x
    path = "../../converted_data/sisFall_dataset/" 
    folders = os.listdir(path)
    
    for folder in folders:
        files = glob.glob(path+folder+"/*.csv")
        for f in files: #get all the csv files
            # if it starts w D, it's an ADL
            OG = pd.read_csv(path+f)
            
            df = pd.DataFrame()
            df["accelerometerAccelerationX(G)"] = OG["accelerometerAccelerationY(G)"]
            df["accelerometerAccelerationY(G)"] = OG["accelerometerAccelerationZ(G)"]
            df["accelerometerAccelerationZ(G)"] = OG["accelerometerAccelerationX(G)"]
            
            sp = f.split("_")
            trial = sp[-1]
            subject = sp[-2]
            
            if("F" in sp[-3]):
                df['Label'] = 1
                num = sp[-3].split("F")[-1]
                num = "F"+num
                
            else:
                df['Label'] = 0
                num = sp[-3].split("D")[-1]
                num = "D"+num
            
            df.to_csv('sup_fall_data/'+subject+"/"+num+"_"+subject+"_"+trial)
            print(num+"_"+subject+"_"+trial)
            

def extract_features():
    #train on 80%
    #30% of the 80% validate
    print("hi")
    
if __name__ == "__main__":
    make_supervised()

            
            
            