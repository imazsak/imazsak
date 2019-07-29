#!/usr/bin/env bash
docker build -t ksisu/imazsak-core .
docker tag ksisu/imazsak-core rg.fr-par.scw.cloud/imazsak/imazsak-core
docker push rg.fr-par.scw.cloud/imazsak/imazsak-core
