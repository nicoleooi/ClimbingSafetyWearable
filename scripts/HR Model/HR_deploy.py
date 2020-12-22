# -*- coding: utf-8 -*-
"""
Created on Mon Dec 21 19:09:08 2020

@author: nicol
"""

from azureml.core import Workspace
from azureml.core.model import Model

model_name = "hr-prediction-lstm"
endpoint_name = "hr-prediction-lstm-ep"

ws = Workspace.from_config()

# Locate the model in the workspace
model = Model(ws, name=model_name)

# Deploy the model as a real-time endpoint
service = Model.deploy(ws, endpoint_name, [model])

# Wait for the model deployment to complete
