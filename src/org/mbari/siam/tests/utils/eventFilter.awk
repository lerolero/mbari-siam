
BEGIN {
  # Initialize printState = 1 to print triggered state data, else initialize 
  # printState = 0 to print detriggered state data.
  printState = 1;

  triggered = 0;
}

/dataStream:/ {
  next;
}

/to TRIGGERED/ {
  triggered = 1;
  next;
}

/to DE-TRIGGERED/ {
  triggered = 0;
  next;
}

{
  if (triggered == printState) {
    print;
  }
}

