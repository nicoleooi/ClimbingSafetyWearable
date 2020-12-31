# Heart Rate Model (HR_LSTM.py)
## Goal:
Given real-time heart rate input, predict the heart rate for the next 1 second on a rolling 50 second window. The first 50 seconds of data input will not receive an immediate prediction. 

`HR_LSTM.py` is the script that runs the training, validation and testing. 
`/models` contains all the best models, quantified by loss. The best model is the last. 
`/models_v2.0` contains all the best models, quantigied by loss. Best model is the last - this is the second round of training on all different types of data.
`HR_custom_deploy_env.py` contains the code to deploy the environment to Azure with required dependencies, creates workspace, endpoint, and model.
`score.py` contains the init() and run() code to teach the Azure container how to initialize the model and how to use input data to predict within the model.
`consume_HRmodel.py` displays an example of POSTing to the ACI endpoint to have the model consume data and predict based off of it. Real time prediction will require sequential json POST requests. 
