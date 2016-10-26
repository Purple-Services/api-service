# Purple API

[![Build Status](https://travis-ci.com/Purple-Services/api-service.svg?token=KufiHsrBkjKnC1q2gM5f&branch=dev)](https://travis-ci.com/Purple-Services/api-service)

## Deploying to Development Server

The server is manually configured with the required System properties in the AWS console. Therefore, the top entry of src/purple/config.clj only sets vars when the environment is "test" or "dev".

Use lein-beanstalk to deploy to AWS ElasticBeanstalk (you must first set up your ~/.lein/profiles.clj with AWS creds):

    lein beanstalk deploy api-dev

## Conventions & Style

Generally, try to follow: https://github.com/bbatsov/clojure-style-guide

Try to keep lines less than 80 columns wide.

## License

Copyright © 2016 Purple Services Inc
