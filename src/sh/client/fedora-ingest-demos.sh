#!/bin/sh

if [ "$FEDORA_HOME" = "" ]; then
  echo "ERROR: Environment variable FEDORA_HOME must be set."
  exit 1
fi

echo "Ingesting Demonstration Objects (18 total)..."

echo "Ingesting local-server simple image demo (1 bdef, 1 bmech, 1 object)..."
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/simple-image-demo/bdef-simple-image.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/simple-image-demo/bmech-simple-image-4res.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/simple-image-demo/obj-image-4res-colliseum.xml "Created by fedora-ingest-demos script")

echo "Ingesting local-server simple document demo (1 object)..."
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/simple-document-demo/obj-document-ECDLpaper.xml "Created by fedora-ingest-demos script")

echo "Ingesting local-server document transform demo (1 bdef, 1 bmech, 1 object)..."
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/document-transform-demo/bdef-document-trans.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/document-transform-demo/bmech-document-trans-saxon.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/document-transform-demo/obj-document-fedoraAPIA.xml "Created by fedora-ingest-demos script")

echo "Ingesting local-server formatting objects demo (2 bdefs, 3 bmechs, 3 objects)..."
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/formatting-objects-demo/bdef-fo.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/formatting-objects-demo/bdef-pdf.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/formatting-objects-demo/bmech-dbx-to-fo.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/formatting-objects-demo/bmech-fop.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/formatting-objects-demo/bmech-tei-to-fo.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/formatting-objects-demo/obj-dbx-to-pdf.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/formatting-objects-demo/obj-fop-to-pdf.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/local-server-demos/formatting-objects-demo/obj-tei-to-pdf.xml "Created by fedora-ingest-demos script")

echo "Ingesting open-server simple image demos (2 bmechs, 2 objects)..."
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/simple-image-demos/bmech-simple-image-4res-zoom.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/simple-image-demos/bmech-simple-image-mrsid.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/simple-image-demos/obj-image-4res-pavilliondraw.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/simple-image-demos/obj-image-mrsid-pavillion.xml "Created by fedora-ingest-demos script")

echo "Ingesting open-server user param image demo (1 bdef, 1 bmech, 2 objects)..."
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/userinput-image-demo/bdef-image-userinput.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/userinput-image-demo/bmech-image-userinput-mrsid.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/userinput-image-demo/obj-image-userinput-archdraw.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/userinput-image-demo/obj-image-userinput-column.xml "Created by fedora-ingest-demos script")

echo "Ingesting open-server EAD finding aid demo (1 bdef, 1 bmech, 1 object)..."
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/EAD-finding-aid-demo/bdef-ead-finding-aid.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/EAD-finding-aid-demo/bmech-ead-finding-aid.xml "Created by fedora-ingest-demos script")
(exec java -Xms64m -Xmx96m -cp $FEDORA_HOME/client:$FEDORA_HOME/client/client.jar -Dfedora.home=$FEDORA_HOME fedora.client.ingest.AutoIngestor $1 $2 $3 $4 $FEDORA_HOME/demo/open-server-demos/EAD-finding-aid-demo/obj-ead-finding-aid.xml "Created by fedora-ingest-demos script")

echo "Finished."

exit 0
