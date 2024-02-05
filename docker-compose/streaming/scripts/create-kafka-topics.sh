#!/bin/sh

pip install --upgrade kafka-python
python /app/scripts/create-kafka-topics.py
