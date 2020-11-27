# Heart Rate Model (HR_LSTM.py)
## Goal:
Given real-time heart rate input, predict the heart rate for the next 1 second on a rolling 50 second window. The first 50 seconds of data input will not receive an immediate prediction. 

`HR_LSTM.py` is the script that runs the training, validation and testing. 
`/models` contains all the best models, quantified by loss. The best model is the last. 