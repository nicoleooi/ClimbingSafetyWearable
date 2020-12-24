# -*- coding: utf-8 -*-
"""
Created on Wed Dec 23 19:51:38 2020

@author: nicol
"""

from azureml.core import Environment
from azureml.core.conda_dependencies import CondaDependencies

env = Environment("my-custom-environment")

env.python.conda_dependencies = CondaDependencies.create(pip_packages=[
    'azureml-defaults>= 1.0.45', # mandatory dependency, contains the functionality needed to host the model as a web service
    'inference-schema[numpy-support]', # dependency for automatic schema generation (for parsing and validating input data)
    'joblib',
    'numpy',
    'pandas',
    'os',
    'glob',
    'time',
    'scikit-learn',
    'keras',
    'json',
    'tensorflow'
])


from azureml.core.model import InferenceConfig

inference_config = InferenceConfig(entry_script="score.py", environment=env)


from azureml.core.webservice import AciWebservice

aci_config = AciWebservice.deploy_configuration(cpu_cores=1, 
                                               memory_gb=1, 
                                               tags={"data": "BPM",  "method" : "tensorflow"}, 
                                               description='Predict heartrate with LSTM')

from azureml.core import Workspace
from azureml.core.model import Model

model_name = "hr-prediction-lstm"
endpoint_name = "hr-prediction-lstm-final-ep"

ws = Workspace.from_config()

model = Model(ws, name=model_name, version=3)

service = Model.deploy(workspace=ws,
                       name=endpoint_name,
                       models=[model],
                       inference_config=inference_config,
                       deployment_config=aci_config)

service.wait_for_deployment(show_output=True)

'''
environment.python.conda_dependencies = CondaDependencies.create(pip_packages=[
    'azureml-defaults>= 1.0.45', # mandatory dependency, contains the functionality needed to host the model as a web service
    'inference-schema[numpy-support]', # dependency for automatic schema generation (for parsing and validating input data)
    'joblib',
    'numpy',
    'pandas',
    'os',
    'glob',
    'time',
    'scikit-learn',
    'keras',
    'json',
    'tensorflow'
])
'''