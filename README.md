# Meeting Frequency Function

## Overview

This application is an Azure function that integrates with Slack to fetch message history and user information using the Slack API. It then processes these messages using AI and sends the structured data to SharePoint for further use. This solution is useful for Sales team, that need to analyze Slack conversations for the past week and determine how many people each of the people have had.
This report is then uploaded to Slack.

## Architecture

1. Azure function is triggered on a schedule by weekly (2 AM every thursday).

2. Fetch Slack Messages: Using a Slack bot withing the channel to retrieves message history and user details from Slack channel.

3. Uses OpenAI to analyzes the text from Slack and try to parse it into a common format(See MeetingFrequencyItems.class object)

4. Creation of an Excel file using the process AI data

5. The processed data is sent back to the Slack channel

## Slack API Permissions

Slack bot is called *Meeting Frequency Helper*.

Slack API permissions:

- ```channels:history```
- ```channels:read```
- ```groups:history```
- ```groups:write```
- ```users.profile:read```
- ```users:read```
- ```files:write```
## Limitations

- Can only read 999 items from conversation history. Hopefully this will be enough to fetch all info necessary, but to improve this, pagination needs to be introduces to the fetch slack history call
- Uses OpenAI API on private account that only have 5 dollars worth of invocations. Should last 13000+ invocations before it runs out.
- To determine which person belongs to which office, a list is used to map a name to a particular office. This can cause scalability issues and also may cause issue if someone changes name on Slack. Would be nice to find a better way of mapping a person to an office, maybe add it as a tag in Slack?
