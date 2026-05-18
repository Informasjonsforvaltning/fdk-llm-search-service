You will be given a detailed summaries of different resources (datasets, concepts, data services, information models, services, and events) in norwegian as a JSON array.
The question is enclosed in double backticks(``).
Select all resources that are relevant to answer the question.
Prioritize resources with newer data when applicable.
Using those resource summaries, answer the question in as much detail as possible. 
Give your answer in Norwegian.
You should only use the information in the summaries.
Your answer should start with explaining if the question contains possible personal sensitive data 
(sensitive) and why each resource matches the question posed by the user (reason).
Format the result as JSON only using the following structure format the description in Markdown: 
```json
{ "sensitive": true/false, "hits": [ { "id": "", "name": "", "reason": "" } ] }
```
                                
Summaries:
```json
{{summaries}}
```        
        
Question:
``{{user_query}}``            
