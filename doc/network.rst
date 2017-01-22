Networked usage
---------------

To use Camelot on multiple computers, the approach is to run Camelot on
one computer, and then connect to that computer over the network from
all other computers needing to use it. That is to say, Camelot itself is
only ever running on one computer, and other computers merely access
Camelot over the network. This is called a Client/Server model.

When Camelot is started from a command line prompt, it will display a
message like:

::

    You might be able to connect to it from the following addresses:
      - http://192.168.1.100:5341/
      - http://localhost.localdomain:5341/

These addresses will change for each computer network Camelot is used
on. Alternatively, you can get the hostname or IP Address of your
computer manually (the process to do this is specific to each operating
system & outside the scope of this guide) and appending ":5341".

In either case, Camelot can be accessed by inputting the network address
into the web browser into another computer on the network.

Using a similar approach, Camelot can be accessed remotely over the
Internet. This often necessitates use of a VPN or similar (configuring a
VPN Server is also outside of the scope of this guide and may require
professional IT services to set up).
