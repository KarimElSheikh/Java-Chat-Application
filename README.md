# Java Network Chat Application

A Java chat application that allows communication between any 2 connected users through socket programming.

The application's structure is as shown in the video: Users connect to intermediate servers which are in turn connected to one  central server. The users can all send direct messages to any user that's connected to any intermediate server. The users need not be aware of their connection to possibly different intermediate servers, as all the interactions between clients, servers, and central server are done seamlessly as needed.

## Information
* The project is developed using NetBeans, the contents of master are the whole NetBeans project's files, including the built Java classes compiled using JDK 8u291.
* If you'll run the project outside NetBeans, you'll need the dependencies found in the artifact "AbsoluteLayout" in the group "org.netbeans.external"

## Demo

A video showing users communicating and sending messages, where the central server and the other servers are relaying the messages as needed.

<a href="https://www.youtube.com/watch?feature=player_embedded&v=Ei6D8WM_KWc
" target="_blank"><img src="https://user-images.githubusercontent.com/10839251/136476338-dbd93cea-a713-4e2e-8b94-16cd53d3869c.png" 
alt="https://www.youtube.com/watch?v=Ei6D8WM_KWc" width="640" height="360" border="10" /></a>