package main

import (
	"bytes"
	"crypto/tls"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strconv"
	"time"
)

func test() {
	fmt.Println("I am runnning task.")
}

func taskWithParams(a int, b string) {
	fmt.Println(a, b)
}

func main() {

	if len(os.Args) < 2 {
		fmt.Println(" Oops ! Not received proper arguments, list of " +
			"arguments are {topic Name, Number of Iterations}")
		os.Exit(1)
	} else {

		var NoofIterationsInString = os.Args[2]
		//fmt.Println(NoofIterationsInString)
		var NoofIterations, err = strconv.Atoi(NoofIterationsInString)
		if err != nil {

		}
		if NoofIterations == -1 {
			NoofIterations = 900000
		}

		queryParams := [12]string{"kube_pod_container_info", "kube_pod_container_status_ready", "kube_pod_container_status_restarts_total", "kube_pod_container_status_terminated", "kube_pod_container_status_terminated_reason", "kube_pod_container_status_waiting", "kube_pod_container_status_waiting_reason", "kube_pod_created", "kube_pod_info", "kube_pod_status_ready", "kube_node_info", "istio_request_count"}
		//fmt.Println(NoofIterations)
		//fmt.Println(err)
		for i := 0; i < NoofIterations; i++ {

			fmt.Println(time.Now())

			var username string = "admin"
			var passwd string = "XOIknQf3gqXrX0rr"
			fmt.Println(username + passwd)
			tr := &http.Transport{
				TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
			}

			client := &http.Client{Transport: tr}
			for query := range queryParams {
				fmt.Println("The query is :: " + queryParams[query])
				fmt.Println(query)
				req, err := http.NewRequest("GET", "http://35.188.208.66:30003/api/v1/query", nil)

				q := req.URL.Query()
				q.Add("query", queryParams[query])
				//q.Add("another_thing", "foo & bar")
				//go in
				req.URL.RawQuery = q.Encode()

				fmt.Println("URL----------------------->" + req.URL.String())
				//fmt.Println(err)
				//req.SetBasicAuth(username, passwd)
				resp, err := client.Do(req)
				if err != nil {
					log.Fatal(err)
				}
				bodyText, err := ioutil.ReadAll(resp.Body)

				var byteR = bytes.NewReader(bodyText)
				//fmt.Println(byteR)
				s := string(bodyText)
				fmt.Println("This is the respose ::::::::" + s)
				//fmt.Println(time.Now())
				//fmt.Println(resp)

				//resp, err = http.Post("http://localhost/api/v1/ingest/analytics", "application/json", byteR)

				req1, err1 := http.NewRequest("POST", "http://127.0.0.1:8082/api/v1/ingest/analytics", byteR)

				req1.Header.Set("content-type", "application/json")
				req1.Header.Set("topic-name", os.Args[1])
				req1.Header.Set("x-auth-header", "abc")
				res, _ := client.Do(req1)

				fmt.Println(res)
				fmt.Println(err1)
			}
			time.Sleep(2 * time.Second)
		}
	}
}
