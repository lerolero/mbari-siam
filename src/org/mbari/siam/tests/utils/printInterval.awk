BEGIN {
}

/^\#/ {
  next;
}

{
  if (NR > 2) {
    printf("sample: %d, interval: %d sec\n", sample++, $1 - prevTime);
  }

  prevTime = $1;
}
