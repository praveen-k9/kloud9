package main

import (
	"bytes"
	"crypto/tls"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	//"strconv"
	"time"
	//"encoding/json"
//	"reflect"
)

func test() {
	fmt.Println("I am runnning task.")
}

func taskWithParams(a int, b string) {
	fmt.Println(a, b)
}

func main() {

	if len(os.Args) < 1 {
		fmt.Println(" Oops ! Not received proper arguments, list of " +
			"arguments are {topic Name, Number of Iterations}")
		os.Exit(1)
	} else {

		// var NoofIterationsInString = os.Args[2]
		// fmt.Println(NoofIterationsInString)
		// var NoofIterations, err = strconv.Atoi(NoofIterationsInString)

		// if NoofIterations == -1 {
		// 	NoofIterations = 900000
		// }

		// fmt.Println(NoofIterations)
		// fmt.Println(err)
		for ; ;  {

			fmt.Println(time.Now())

			var username string = "admin"
			var   passwd string = "biuvuQ2KMmKwIrHk"
			tr := &http.Transport{
				TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
			}

			client := &http.Client{Transport: tr}

			/* GCP */

		        // Stats Summary
			// gke-demo-cluster-default-pool-df76e3f3-5tl4	
			//req, err := http.NewRequest("GET", "http://35.232.80.168:10255/stats/summary", nil)

			// gke-demo-cluster-default-pool-df76e3f3-gwm9
			//req, err := http.NewRequest("GET", "http://35.188.168.62:10255/stats/summary", nil)
			
			// Requests
			req, err := http.NewRequest("GET", "https://35.224.86.126/api/v1/pods/", nil)

			// Capacity
			//req, err := http.NewRequest("GET", "https://35.224.86.126/api/v1/nodes/gke-demo-cluster-default-pool-df76e3f3-5tl4/status", nil)
			//req, err := http.NewRequest("GET", "https://35.224.86.126/api/v1/nodes/gke-demo-cluster-default-pool-df76e3f3-gwm9/status", nil)

			//req, err := http.NewRequest("GET", "https://35.224.86.126/logs/kube-apiserver.log", nil)

			req.SetBasicAuth(username, passwd)
			resp, err := client.Do(req)
			if err != nil {
				log.Fatal(err)
			}
			bodyText, err := ioutil.ReadAll(resp.Body)

			var byteR = bytes.NewReader(bodyText)

			s := string(bodyText)
			fmt.Println(s)
			fmt.Println(time.Now())
			fmt.Println(resp)

			//resp, err = http.Post("http://localhost/api/v1/ingest/analytics", "application/json", byteR)

			req1, err1 := http.NewRequest("POST", "http://localhost:8084/api/v1/ingest/analytics", byteR)

			req1.Header.Set("content-type", "application/json")
			//req1.Header.Set("content-type", "text/plain")
			req1.Header.Set("topic-name", os.Args[1])
			req1.Header.Set("x-auth-header", "abc")
			res, _ := client.Do(req1)

			fmt.Println("***************Result*************\n",res)
			fmt.Println("**********Error Occurred *****************\n",err1)
		}

	}
}
